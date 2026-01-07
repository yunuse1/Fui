# FUI - Docker, Vaadin ve Android notları

Bu proje artık çok modüllüdür (server + common + vaadin + androidApp). Aşağıda Docker ile çalıştırma ve Android/Vaadin geliştirme notları bulunmaktadır.

## Docker (önerilen geliştirme/CI yolu)
- Docker container içinde Temurin 21 JRE kullanılarak uygulama çalıştırılır. Host makinenin JDK sürümüne bağlı kalmadan container çalışır.

Yapmak için:
1. Proje kökünde server jar oluşturun:
```bash
./gradlew shadowJar -x test
```
2. Docker image oluşturun ve çalıştırın:
```bash
docker build -t fui-server .
docker run -p 8080:8080 --rm fui-server
```
3. Endpoint testi:
- Health: http://localhost:8080/health
- Image test: http://localhost:8080/image/test-analyze veya http://localhost:8080/test-analyze

## Vaadin
- `vaadin` modülü içinde bir örnek `MainView` ekledim. Vaadin uygulamasını embedded bir container içinde çalıştırmak için ek yapılandırma gereklidir.
- Lokal geliştirme için Vaadin plugin veya Spring Boot ile entegre etme seçenekleri tercih edilebilir.

## Android
- `androidApp` modülü temel bir yapı ile eklendi (placeholder). Android Studio ile açıp SDK/NDK ayarlarını yaptıktan sonra derleyip cihazda veya emulatorde çalıştırabilirsiniz.
- Android derlemesi CI ortamında veya developer makinesinde Android SDK gerektirir.

## Not
- Önceden `gradle.properties` içinde sabit `org.gradle.java.home` bulunuyordu; host üzerinde JDK kurulumu zorunluluğunu hafifletmek için bu satırı yorum satırı yaptım. Geliştiriciler isterlerse kendi makinelerinde JDK21/17 kurup `org.gradle.java.home` ayarını kullanabilirler.


