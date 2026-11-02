# NetVault Private Backend API (Zero SQL, Zero Browser Admin)

Sistem backend NetVault dirancang khusus untuk melayani aplikasi Android secara aman tanpa website admin browser dan tanpa database SQL.

## 1. Struktur Direktori Backend

```text
/backend/
├── index.php             # Router utama & status kesehatan API
├── lib/
│   ├── Storage.php       # Mesin penyimpanan berkas JSON atomik (file locking, .tmp & .bak)
│   └── Security.php      # Validasi token Bearer, rate limiting, sanitasi path traversal
├── api/
│   ├── auth.php          # Registrasi username & penerbitan token sesi
│   ├── config.php        # Konfigurasi remote dinamis & pembaruan admin
│   ├── vault.php         # Sinkronisasi metadata & transfer berkas terenkripsi
│   └── stats.php         # Telemetri agregat & analitik administrator
├── data/                 # Direktori penyimpanan privat (DIKUNCI DARI AKSES PUBLIK)
│   ├── users.json
│   ├── sessions.json
│   ├── global_config.json
│   ├── telemetry.json
│   ├── rate_limits.json
│   └── vault_files/      # Folder berkas terenkripsi per-user (IDOR protected)
│       └── {username}/
│           └── {fileId}.enc
└── README.md
```

## 2. Arsitektur Penyimpanan Berkas JSON Atomik

Sesuai aturan ketat: **Tidak menggunakan SQL (MySQL, PostgreSQL, SQLite, dsb.)**.
Untuk mencegah korupsi data saat penulisan konkuren:
1. **File Locking (`flock`)**: Menggunakan `LOCK_EX` saat menulis dan `LOCK_SH` saat membaca.
2. **Penulisan Atomik (.tmp & rename)**: Data JSON ditulis ke berkas sementara `.tmp.[id]`, dilakukan `fflush()`, lalu di-rename secara atomik ke target `.json`.
3. **Pencadangan Otomatis (.bak)**: Sebelum penulisan baru dilakukan, berkas sebelumnya disalin ke `.bak`. Jika berkas utama rusak atau kosong, sistem otomatis memulihkan dari cadangan.

## 3. Dokumentasi Endpoint API

| Endpoint | Metode | Autentikasi | Fungsi |
|---|---|---|---|
| `/index.php` | GET | Publik | Status kesehatan sistem & daftar endpoint |
| `/api/auth.php` | POST | Publik | Registrasi username & penerbitan token sesi 30 hari |
| `/api/config.php` | GET | Publik | Mengambil konfigurasi remote dinamis aplikasi |
| `/api/config.php` | POST | Bearer (ADMIN) | Memperbarui konfigurasi remote global |
| `/api/vault.php?action=sync_metadata` | POST | Bearer | Sinkronisasi daftar berkas cloud untuk user terkait |
| `/api/vault.php?action=upload_file` | POST (Multipart) | Bearer | Unggah berkas terenkripsi AES-256 (Maks 100MB per berkas) |
| `/api/vault.php?action=download_file` | GET | Bearer | Unduh berkas terenkripsi milik user sendiri |
| `/api/vault.php?action=delete_file` | POST | Bearer | Hapus berkas terenkripsi dari cloud vault |
| `/api/stats.php` | POST | Publik/User | Mengirim metrik agregat kalkulasi & transfer data |
| `/api/stats.php` | GET | Bearer (ADMIN) | Mengambil statistik DAU/WAU/MAU & riwayat audit |

## 4. Keamanan & Proteksi IDOR (Insecure Direct Object Reference)

- **Isolasi Folder**: Berkas terenkripsi disimpan dalam `/data/vault_files/{safe_username}/`. User hanya dapat mengunduh atau menghapus berkas di dalam subdirektori miliknya.
- **Sanitasi Path Traversal**: Seluruh parameter `fileId` dan `username` disaring menggunakan regex ketat `[^a-zA-Z0-9_\-\.]` untuk mencegah serangan `../`.
- **Proteksi Akses Web Server**: Direktori `/backend/data/` wajib diblokir dari akses HTTP langsung melalui `.htaccess` atau konfigurasi Nginx.

## 5. Panduan Konfigurasi HTTPS & Nginx

Tambahkan blok berikut pada konfigurasi server Nginx Anda:

```nginx
server {
    listen 443 ssl http2;
    server_name api.netvault.domain;

    ssl_certificate /etc/letsencrypt/live/api.netvault.domain/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.netvault.domain/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    root /var/www/netvault/backend;
    index index.php;

    client_max_body_size 105M;

    # Kunci direktori data privat agar tidak dapat diakses dari browser
    location ^~ /data/ {
        deny all;
        return 404;
    }

    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
    }
}
```
