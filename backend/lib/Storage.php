<?php
/**
 * NetVault Backend - Atomic JSON Storage Engine
 * Enforces file locking, atomic writes with .tmp and rename, and .bak backups.
 * NO SQL DATABASE USED.
 */

class JsonStorage {
    private static $baseDir = __DIR__ . '/../data/';

    private static function init() {
        if (!is_dir(self::$baseDir)) {
            mkdir(self::$baseDir, 0750, true);
        }
    }

    public static function read($filename, $default = []) {
        self::init();
        $filePath = self::$baseDir . $filename;
        $bakPath = $filePath . '.bak';

        if (!file_exists($filePath)) {
            if (file_exists($bakPath)) {
                $content = @file_get_contents($bakPath);
                $decoded = json_decode($content, true);
                if ($decoded !== null) {
                    @copy($bakPath, $filePath);
                    return $decoded;
                }
            }
            return $default;
        }

        $fp = @fopen($filePath, 'r');
        if (!$fp) return $default;

        flock($fp, LOCK_SH);
        $content = stream_get_contents($fp);
        flock($fp, LOCK_UN);
        fclose($fp);

        $decoded = json_decode($content, true);
        if ($decoded === null && file_exists($bakPath)) {
            $bakContent = @file_get_contents($bakPath);
            $bakDecoded = json_decode($bakContent, true);
            if ($bakDecoded !== null) {
                @copy($bakPath, $filePath);
                return $bakDecoded;
            }
            return $default;
        }

        return $decoded !== null ? $decoded : $default;
    }

    public static function write($filename, $data) {
        self::init();
        $filePath = self::$baseDir . $filename;
        $tmpPath = $filePath . '.tmp.' . uniqid('', true);
        $bakPath = $filePath . '.bak';

        $json = json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        if ($json === false) return false;

        // Backup existing file
        if (file_exists($filePath) && filesize($filePath) > 0) {
            @copy($filePath, $bakPath);
        }

        // Write to temp file with exclusive lock
        $fp = @fopen($tmpPath, 'w');
        if (!$fp) return false;

        flock($fp, LOCK_EX);
        fwrite($fp, $json);
        fflush($fp);
        flock($fp, LOCK_UN);
        fclose($fp);

        // Atomic rename
        $renamed = @rename($tmpPath, $filePath);
        if (!$renamed) {
            @copy($tmpPath, $filePath);
            @unlink($tmpPath);
        }

        return true;
    }
}
