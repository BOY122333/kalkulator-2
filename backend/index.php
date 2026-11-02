<?php
/**
 * NetVault Backend API Router & Health Check
 * Pure API Gateway - NO Browser Admin Website
 */

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('X-XSS-Protection: 1; mode=block');

echo json_encode([
    'service' => 'NetVault Backend API',
    'status' => 'ONLINE',
    'engine' => 'Atomic JSON Storage (Zero SQL)',
    'version' => '1.0.0',
    'endpoints' => [
        'POST /api/auth.php' => 'Username registration & token issuance',
        'GET /api/config.php' => 'Dynamic remote application configuration',
        'POST /api/config.php' => 'Admin remote configuration update',
        'POST /api/vault.php?action=upload_file' => 'Encrypted private file upload',
        'GET /api/vault.php?action=download_file' => 'Encrypted private file download',
        'POST /api/vault.php?action=sync_metadata' => 'Metadata sync',
        'POST /api/stats.php' => 'Submit telemetry metrics',
        'GET /api/stats.php' => 'Administrator analytics & audit logs'
    ],
    'server_time' => date('Y-m-d H:i:s T')
], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
