<?php
/**
 * NetVault Backend - Security Utilities
 * Handles Bearer token validation, rate limiting, and path traversal prevention.
 */

require_once __DIR__ . '/Storage.php';

class Security {

    public static function sendJson($data, $statusCode = 200) {
        http_response_code($statusCode);
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
        exit;
    }

    public static function sanitizeIdentifier($id) {
        return preg_replace('/[^a-zA-Z0-9_\-\.]/', '', $id);
    }

    public static function checkRateLimit($key, $maxRequests = 60, $windowSeconds = 60) {
        $rates = JsonStorage::read('rate_limits.json', []);
        $now = time();
        $safeKey = self::sanitizeIdentifier($key);

        if (!isset($rates[$safeKey])) {
            $rates[$safeKey] = ['count' => 1, 'start' => $now];
        } else {
            if ($now - $rates[$safeKey]['start'] > $windowSeconds) {
                $rates[$safeKey] = ['count' => 1, 'start' => $now];
            } else {
                $rates[$safeKey]['count']++;
                if ($rates[$safeKey]['count'] > $maxRequests) {
                    self::sendJson(['status' => 'error', 'message' => 'Terlalu banyak permintaan (Rate limit exceeded).'], 429);
                }
            }
        }
        JsonStorage::write('rate_limits.json', $rates);
    }

    public static function getBearerToken() {
        $headers = function_exists('apache_request_headers') ? apache_request_headers() : [];
        $auth = $headers['Authorization'] ?? $headers['authorization'] ?? $_SERVER['HTTP_AUTHORIZATION'] ?? '';
        if (preg_match('/Bearer\s(\S+)/', $auth, $matches)) {
            return $matches[1];
        }
        return null;
    }

    public static function authenticateUser() {
        $token = self::getBearerToken();
        if (!$token) {
            self::sendJson(['status' => 'error', 'message' => 'Token otentikasi tidak ditemukan.'], 401);
        }

        $sessions = JsonStorage::read('sessions.json', []);
        foreach ($sessions as $username => $session) {
            if (($session['token'] ?? '') === $token) {
                if (time() > ($session['expires_at'] ?? 0)) {
                    self::sendJson(['status' => 'error', 'message' => 'Sesi telah kedaluwarsa. Silakan masuk kembali.'], 401);
                }
                return [
                    'username' => $username,
                    'role' => $session['role'] ?? 'USER',
                    'deviceId' => $session['deviceId'] ?? ''
                ];
            }
        }

        // Demo fallback token for direct local testing
        if (str_starts_with($token, 'demo_token_')) {
            $user = substr($token, strlen('demo_token_'));
            return ['username' => self::sanitizeIdentifier($user), 'role' => 'USER', 'deviceId' => 'demo-device'];
        }

        self::sendJson(['status' => 'error', 'message' => 'Token otentikasi tidak valid.'], 403);
    }

    public static function requireAdmin() {
        $user = self::authenticateUser();
        if (!in_array($user['role'], ['ADMIN', 'OWNER'])) {
            self::sendJson(['status' => 'error', 'message' => 'Akses ditolak: Membutuhkan izin Administrator.'], 403);
        }
        return $user;
    }
}
