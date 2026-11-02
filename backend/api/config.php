<?php
/**
 * NetVault Backend - Remote Config API
 * GET /api/config.php: Returns global dynamic remote config
 * POST /api/config.php: Updates global dynamic config (requires ADMIN role)
 */

require_once __DIR__ . '/../lib/Security.php';
require_once __DIR__ . '/../lib/Storage.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

$defaultConfig = [
    'version' => 1,
    'appDisplayName' => 'NetVault',
    'homeTitle' => 'Kalkulator IP Subnet & Vault',
    'appDescription' => 'Kalkulator IP Subnet Profesional, Private Vault Terenkripsi AES-256, dan App Vault Terintegrasi.',
    'primaryColorHex' => '#0284C7',
    'accentColorHex' => '#38BDF8',
    'announcementMessage' => 'NetVault v1.0 siap digunakan. Akses Ruang Rahasia terlindungi enkripsi AES-256.',
    'helpText' => 'Gunakan tab Subnet untuk menghitung CIDR /0-/32. Akses Ruang Rahasia dengan PIN atau Biometrik.',
    'maxUserStorageMb' => 1024,
    'openingMode' => 'NONE',
    'openingDurationMs' => 2500,
    'openingSkipAllowed' => true,
    'openingAssetUrl' => null,
    'lastUpdated' => time() * 1000
];

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $config = JsonStorage::read('global_config.json', $defaultConfig);
    header('Cache-Control: public, max-age=60');
    Security::sendJson($config);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $user = Security::requireAdmin();
    $raw = file_get_contents('php://input');
    $newConfig = json_decode($raw, true);

    if (!is_array($newConfig)) {
        Security::sendJson(['status' => 'error', 'message' => 'Format payload konfigurasi tidak valid.'], 400);
    }

    $current = JsonStorage::read('global_config.json', $defaultConfig);
    $merged = array_merge($current, $newConfig);
    $merged['version'] = ($current['version'] ?? 1) + 1;
    $merged['lastUpdated'] = time() * 1000;

    JsonStorage::write('global_config.json', $merged);

    // Audit log
    $stats = JsonStorage::read('telemetry.json', ['audit_logs' => []]);
    $stats['audit_logs'][] = [
        'id' => uniqid('audit_', true),
        'timestamp' => time() * 1000,
        'eventType' => 'REMOTE_CONFIG_UPDATED',
        'username' => $user['username'],
        'details' => 'Konfigurasi global diperbarui ke versi ' . $merged['version'],
        'ipAddressObserved' => $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1'
    ];
    JsonStorage::write('telemetry.json', $stats);

    Security::sendJson(['status' => 'success', 'message' => 'Konfigurasi global berhasil disimpan.', 'config' => $merged]);
}
