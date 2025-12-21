# Federated Urban Insights - Local Prototype

Bu repo Federated Urban Insights (FUI) projesinin minimal local prototipini içerir: Ktor tabanlı backend, basit ingest endpoint ve Docker ile PostgreSQL.

Nasıl çalıştırılır (geliştirici makinesi):

1. Gerekli araçlar:
   - JDK 17+
   - Gradle (veya `./gradlew` wrapper)
   - Docker & Docker Compose

2. Projeyi build et ve çalıştır:

```powershell
# Windows PowerShell
./gradlew build
# docker-compose ile çalıştır
docker-compose up --build
docker-compose up -d --build // working in background

$bytes = [IO.File]::ReadAllBytes("C:\Users\Yunus\Desktop\indir.jpg") 
$base64 = [Convert]::ToBase64String($bytes)
$base64 | Set-Clipboard
```

3. Sağlık kontrolü:

GET http://localhost:8080/health

4. Örnek ingest isteği:

POST http://localhost:8080/ingest
Content-Type: application/json

```json
{
  "imageBase64": "CTRL+V İLE YAPIŞTIR",
  "filename": "otobus.jpg",
  "enableVehicleDetection": true,
  "enableCrowdAnalysis": true,
  "enableAirQuality": true,
  "enableTrafficAnalysis": true
}

```


