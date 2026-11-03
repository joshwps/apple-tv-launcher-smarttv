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



## ✨ Funcionalidades e Guia de Uso

O launcher foi desenhado para ser totalmente personalizável usando o controle remoto da TV. Abaixo estão todas as ações que você pode realizar:

### 🎮 Navegação e Controle
- **Navegar:** Use as setas (cima, baixo, esquerda, direita) do controle para mover o foco entre os aplicativos da Dock (barra inferior) e da Grade.
- **Abrir App:** Aperte o botão **OK** rapidamente para abrir o aplicativo selecionado.
- **Menu de Opções (Clique Longo):** Com o foco sobre um aplicativo (na Dock ou na Grade), **pressione e segure o botão OK** para abrir o menu de personalização. Lá você pode:
  - Renomear o aplicativo.
  - Alterar o ícone (escolhendo uma imagem do armazenamento da TV).
  - Restaurar o nome e o ícone original de fábrica.

### 🎨 Personalização Visual
- **Papel de Parede (Wallpaper):** Altere o plano de fundo acessando as configurações do launcher. Você pode escolher imagens estáticas da sua TV.
- **Fontes e Interface:** Interface otimizada com fontes mais nítidas, grossas e com sombras para melhor leitura a distância.

### 🌤️ Clima e Relógio
- **Widget de Clima:** Configure sua cidade (ex: São Paulo) e veja a temperatura em tempo real e a condição do tempo (Open-Meteo API).
- **Overlay de Relógio:** Ative ou desative o relógio flutuante que aparece sobre os aplicativos abertos.
- **Posição do Overlay:** Escolha em qual canto da tela o relógio vai aparecer quando você abrir o YouTube, Netflix, etc.
- **Formato do Relógio:** Altere entre formato 12h ou 24h, e ative/desative a exibição dos segundos.

### 💾 Sistema e Backup
- **Backup do Layout:** Salve **tudo** (posições, ícones personalizados, nomes, wallpaper, clima) em um arquivo `.zip` seguro.
- **Restauração:** Restaure o layout completo em um clique. Perfeito para trocar de TV ou recuperar uma configuração perdida.
- **Launcher Padrão:** Defina o app como a tela inicial do sistema (ao apertar o botão Home do controle).

### 🛠️ Como Compilar (Sem Android Studio)
## 🛠️ Como Compilar (Sem Android Studio)
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
