# 📺 Apple TV Launcher (Smart TV)


## 📸 Screenshots

### 🏠 Tela Inicial
<img src="screenshots/tela_inicial.png" width="800" alt="Tela Inicial">

### 📱 Grade de Aplicativos
<img src="screenshots/grade_apps.png" width="800" alt="Grade de Aplicativos">

### ⚙️ Menu de Configurações
<img src="screenshots/menu_configuracoes.png" width="800" alt="Menu de Configurações">

### ☀️ Menu do Clima
<img src="screenshots/menu_clima.png" width="800" alt="Menu do Clima">

---
Um launcher customizado e moderno para Smart TVs rodando **Android 9+** (otimizado para Android 14). Inspirado na interface limpa da Apple TV. 

> **Créditos:** Este projeto foi originalmente baseado no código de [Kulwant Nagi](https://github.com/kulwantnagi) e foi profundamente modificado, otimizado e adaptado por [Joshwps](https://github.com/joshwps).

## ✨ Funcionalidades

- **Interface Premium:** Design minimalista com fontes nítidas e sombras.
- **Dock Ajustável:** Mude a barra de aplicativos entre a base e o topo da tela.
- **Widget de Clima:** Integração com a API Open-Meteo.
- **Overlay de Relógio:** Relógio flutuante sobre os apps.
- **Backup/Restauração em ZIP:** Salva layout e ícones na pasta `Download`.
- **Otimização:** Desenvolvido e otimizado especificamente para o Android 14 (Smart TV).

## 🛠️ Como Compilar (Sem Android Studio)

Você só precisa do **Java (JDK 17)** e do **Android SDK** configurados no seu Linux.

```bash
git clone https://github.com/joshwps/apple-tv-launcher-smarttv.git
cd apple-tv-launcher-smarttv/launcher
./gradlew assembleDebug
```

O APK será gerado em: `launcher/app/build/outputs/apk/debug/app-debug.apk`

## 📲 Como Instalar na TV

Com a TV conectada via ADB, rode:

```bash
adb install -r launcher/app/build/outputs/apk/debug/app-debug.apk
```

## 📄 Licença

Este projeto está licenciado sob a Licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.
