<?php
/**
 * NetVault Backend - Statistics & Telemetry API
 * POST /api/stats.php: Submits aggregate anonymous usage metrics (no file contents!)
 * GET /api/stats.php: Retrieves complete analytics for authenticated Administrator
 */

require_once __DIR__ . '/../lib/Security.php';
require_once __DIR__ . '/../lib/Storage.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// 1. Submit Client Telemetry (POST)
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $raw = file_get_contents('php://input');
    $body = json_decode($raw, true) ?? [];

    $username = trim($body['username'] ?? 'anonim');
    $calcCount = intval($body['calculationCount'] ?? 0);
    $upBytes = intval($body['uploadedBytes'] ?? 0);
    $downBytes = intval($body['downloadedBytes'] ?? 0);

    $stats = JsonStorage::read('telemetry.json', [
        'registeredUsersCount' => 0,
        'activeUsersDaily' => 0,
        'activeUsersWeekly' => 0,
        'activeUsersMonthly' => 0,
        'totalCalculations' => 0,
        'totalCloudUploads' => 0,
        'totalCloudDownloads' => 0,
        'totalCloudBytes' => 0,
        'auditLogs' => []
    ]);

    $stats['totalCalculations'] = ($stats['totalCalculations'] ?? 0) + $calcCount;
    $stats['totalCloudBytes'] = ($stats['totalCloudBytes'] ?? 0) + $upBytes + $downBytes;

    JsonStorage::write('telemetry.json', $stats);
    Security::sendJson(['status' => 'success', 'message' => 'Telemetri berhasil dicatat.']);
}

// 2. Get Admin Analytics (GET - Requires ADMIN role)
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $admin = Security::requireAdmin();

    $users = JsonStorage::read('users.json', []);
    $stats = JsonStorage::read('telemetry.json', [
        'totalCalculations' => 0,
        'totalCloudUploads' => 0,
        'totalCloudDownloads' => 0,
        'totalCloudBytes' => 0,
        'audit_logs' => []
    ]);

    $now = time();
    $dailyActive = 0;
    $weeklyActive = 0;
    $monthlyActive = 0;

    foreach ($users as $u) {
        $lastActive = $u['last_active_at'] ?? 0;
        if ($now - $lastActive <= 86400) $dailyActive++;
        if ($now - $lastActive <= 604800) $weeklyActive++;
        if ($now - $lastActive <= 2592000) $monthlyActive++;
    }

    $response = [
        'registeredUsersCount' => count($users),
        'activeUsersDaily' => max(1, $dailyActive),
        'activeUsersWeekly' => max(1, $weeklyActive),
        'activeUsersMonthly' => max(1, $monthlyActive),
        'totalCalculations' => $stats['totalCalculations'] ?? 0,
        'totalCloudUploads' => $stats['totalCloudUploads'] ?? 0,
        'totalCloudDownloads' => $stats['totalCloudDownloads'] ?? 0,
        'totalCloudBytes' => $stats['totalCloudBytes'] ?? 0,
        'auditLogs' => array_slice(array_reverse($stats['audit_logs'] ?? []), 0, 50)
    ];

    Security::sendJson($response);
}
