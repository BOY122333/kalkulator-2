<?php
/**
 * NetVault Backend - Auth API
 * POST /api/auth.php
 * Handles username registration, session issuance, and device binding without passwords or SQL.
 */

require_once __DIR__ . '/../lib/Security.php';
require_once __DIR__ . '/../lib/Storage.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

$clientIp = $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1';
Security::checkRateLimit('auth_' . $clientIp, 30, 60);

$raw = file_get_contents('php://input');
$body = json_decode($raw, true) ?? [];

$username = trim($body['username'] ?? '');
$deviceId = trim($body['deviceId'] ?? '');

if (strlen($username) < 3 || strlen($username) > 20 || !preg_match('/^[a-zA-Z0-9_]+$/', $username)) {
    Security::sendJson(['status' => 'error', 'message' => 'Username tidak valid (harus 3-20 karakter alfanumerik).'], 400);
}

if (empty($deviceId)) {
    Security::sendJson(['status' => 'error', 'message' => 'Device ID wajib disertakan.'], 400);
}

$safeUser = Security::sanitizeIdentifier($username);
$users = JsonStorage::read('users.json', []);
$sessions = JsonStorage::read('sessions.json', []);

$now = time();
$isNew = !isset($users[$safeUser]);

if ($isNew) {
    // Register new user
    $role = (count($users) === 0) ? 'OWNER' : 'USER'; // First registered user is system OWNER
    $users[$safeUser] = [
        'username' => $safeUser,
        'bound_devices' => [$deviceId],
        'role' => $role,
        'created_at' => $now,
        'last_active_at' => $now
    ];
} else {
    // Existing user: verify device binding
    $role = $users[$safeUser]['role'] ?? 'USER';
    if (!in_array($deviceId, $users[$safeUser]['bound_devices'] ?? [])) {
        $users[$safeUser]['bound_devices'][] = $deviceId;
    }
    $users[$safeUser]['last_active_at'] = $now;
}

// Issue session token
$sessionToken = bin2hex(random_bytes(32));
$sessions[$safeUser] = [
    'token' => $sessionToken,
    'deviceId' => $deviceId,
    'role' => $role,
    'issued_at' => $now,
    'expires_at' => $now + (86400 * 30) // 30 days
];

JsonStorage::write('users.json', $users);
JsonStorage::write('sessions.json', $sessions);

Security::sendJson([
    'status' => 'success',
    'message' => $isNew ? 'Pengguna baru berhasil didaftarkan.' : 'Sesi pengguna berhasil diverifikasi.',
    'sessionToken' => $sessionToken,
    'role' => $role,
    'registeredAt' => $users[$safeUser]['created_at']
]);
