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
```

3. Sağlık kontrolü:

GET http://localhost:8080/health

4. Örnek ingest isteği:

POST http://localhost:8080/ingest
Content-Type: application/json

```json
{
  "deviceId": "device-1",
  "timestamp": 1690000000000,
  "metrics": { "noise": 72.3, "pm25": 12.1 }
}
```


