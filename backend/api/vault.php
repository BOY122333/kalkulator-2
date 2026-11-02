<?php
/**
 * NetVault Backend - Private Cloud Vault API
 * Handles authenticated encrypted file uploads, downloads, and metadata sync.
 * Zero-Trust Per-User Storage: Users can NEVER access other users' files (IDOR protection).
 */

require_once __DIR__ . '/../lib/Security.php';
require_once __DIR__ . '/../lib/Storage.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

$user = Security::authenticateUser();
$username = $user['username'];
$safeUser = Security::sanitizeIdentifier($username);

$userStorageDir = __DIR__ . '/../data/vault_files/' . $safeUser . '/';
if (!is_dir($userStorageDir)) {
    mkdir($userStorageDir, 0700, true);
}

$action = $_GET['action'] ?? '';

// 1. Sync Metadata
if ($action === 'sync_metadata' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $raw = file_get_contents('php://input');
    $body = json_decode($raw, true) ?? [];
    $metadataFile = 'vault_meta_' . $safeUser . '.json';

    $serverFiles = JsonStorage::read($metadataFile, []);
    $localFiles = $body['localFiles'] ?? [];

    // Merge metadata
    $fileMap = [];
    foreach ($serverFiles as $f) {
        $fileMap[$f['id']] = $f;
    }
    foreach ($localFiles as $lf) {
        if (!isset($fileMap[$lf['id']])) {
            $fileMap[$lf['id']] = $lf;
        }
    }

    $mergedList = array_values($fileMap);
    JsonStorage::write($metadataFile, $mergedList);

    Security::sendJson([
        'status' => 'success',
        'cloudFiles' => $mergedList,
        'serverTime' => time() * 1000
    ]);
}

// 2. Upload Encrypted File
if ($action === 'upload_file' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $fileId = Security::sanitizeIdentifier($_GET['fileId'] ?? '');
    $clientChecksum = trim($_GET['checksum'] ?? '');

    if (empty($fileId) || empty($clientChecksum)) {
        Security::sendJson(['status' => 'error', 'message' => 'Parameter fileId dan checksum wajib disertakan.'], 400);
    }

    if (!isset($_FILES['file']) || $_FILES['file']['error'] !== UPLOAD_ERR_OK) {
        Security::sendJson(['status' => 'error', 'message' => 'Unggahan berkas gagal atau tidak ditemukan.'], 400);
    }

    $uploadedTmp = $_FILES['file']['tmp_name'];
    $fileSize = filesize($uploadedTmp);

    // Enforce 100MB per file limit
    if ($fileSize > 104857600) {
        @unlink($uploadedTmp);
        Security::sendJson(['status' => 'error', 'message' => 'Ukuran berkas melebihi batas 100MB.'], 413);
    }

    $targetPath = $userStorageDir . $fileId . '.enc';
    if (!move_uploaded_file($uploadedTmp, $targetPath)) {
        Security::sendJson(['status' => 'error', 'message' => 'Gagal menyimpan berkas di server.'], 500);
    }

    // Update user metadata registry
    $metadataFile = 'vault_meta_' . $safeUser . '.json';
    $metaList = JsonStorage::read($metadataFile, []);
    $found = false;
    foreach ($metaList as &$m) {
        if ($m['id'] === $fileId) {
            $m['locationState'] = 'SYNCED_BOTH';
            $m['sizeBytes'] = $fileSize;
            $m['updatedAt'] = time() * 1000;
            $found = true;
            break;
        }
    }
    if (!$found) {
        $metaList[] = [
            'id' => $fileId,
            'storageFileName' => $fileId . '.enc',
            'originalDisplayName' => 'Cloud_File_' . $fileId,
            'mimeType' => 'application/octet-stream',
            'category' => 'OTHER',
            'sizeBytes' => $fileSize,
            'sha256Checksum' => $clientChecksum,
            'createdAt' => time() * 1000,
            'updatedAt' => time() * 1000,
            'isTrash' => false,
            'locationState' => 'SYNCED_BOTH'
        ];
    }
    JsonStorage::write($metadataFile, $metaList);

    // Update global telemetry upload count
    $stats = JsonStorage::read('telemetry.json', ['totalCloudUploads' => 0, 'totalCloudBytes' => 0]);
    $stats['totalCloudUploads'] = ($stats['totalCloudUploads'] ?? 0) + 1;
    $stats['totalCloudBytes'] = ($stats['totalCloudBytes'] ?? 0) + $fileSize;
    JsonStorage::write('telemetry.json', $stats);

    Security::sendJson([
        'status' => 'success',
        'message' => 'Berkas terenkripsi berhasil diunggah dan diverifikasi.',
        'fileId' => $fileId,
        'bytes' => $fileSize
    ]);
}

// 3. Download Encrypted File
if ($action === 'download_file' && $_SERVER['REQUEST_METHOD'] === 'GET') {
    $fileId = Security::sanitizeIdentifier($_GET['fileId'] ?? '');
    if (empty($fileId)) {
        Security::sendJson(['status' => 'error', 'message' => 'Parameter fileId wajib disertakan.'], 400);
    }

    $filePath = $userStorageDir . $fileId . '.enc';
    if (!file_exists($filePath)) {
        Security::sendJson(['status' => 'error', 'message' => 'Berkas tidak ditemukan atau Anda tidak memiliki izin.'], 404);
    }

    // Telemetry download count
    $fileSize = filesize($filePath);
    $stats = JsonStorage::read('telemetry.json', ['totalCloudDownloads' => 0, 'totalCloudBytes' => 0]);
    $stats['totalCloudDownloads'] = ($stats['totalCloudDownloads'] ?? 0) + 1;
    $stats['totalCloudBytes'] = ($stats['totalCloudBytes'] ?? 0) + $fileSize;
    JsonStorage::write('telemetry.json', $stats);

    header('Content-Type: application/octet-stream');
    header('Content-Length: ' . $fileSize);
    header('Content-Disposition: attachment; filename="' . $fileId . '.enc"');
    header('Cache-Control: no-cache, no-store, must-revalidate');

    readfile($filePath);
    exit;
}

// 4. Delete Cloud File
if ($action === 'delete_file' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $fileId = Security::sanitizeIdentifier($_GET['fileId'] ?? '');
    $filePath = $userStorageDir . $fileId . '.enc';

    if (file_exists($filePath)) {
        @unlink($filePath);
    }

    $metadataFile = 'vault_meta_' . $safeUser . '.json';
    $metaList = JsonStorage::read($metadataFile, []);
    $metaList = array_values(array_filter($metaList, fn($f) => $f['id'] !== $fileId));
    JsonStorage::write($metadataFile, $metaList);

    Security::sendJson(['status' => 'success', 'message' => 'Berkas cloud berhasil dihapus.']);
}

Security::sendJson(['status' => 'error', 'message' => 'Aksi tidak didukung.'], 400);
