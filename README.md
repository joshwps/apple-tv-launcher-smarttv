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

### 🎨 Personalização de Aplicativos
- **Renomear Apps:** Mude o nome de qualquer aplicativo na Dock ou na Grade principal.
- **Alterar Ícones:** Substitua o ícone padrão de qualquer app por uma imagem personalizada (pode ser um arquivo da TV ou uma imagem enviada do seu celular).
- **Restaurar Padrão:** Volte o nome e o ícone de um app para o original de fábrica a qualquer momento.
- **Organização:** Adicione, remova e mova aplicativos entre a Dock (barra principal) e a Grade.

### 🖼️ Aparência e Layout
- **Papel de Parede (Wallpaper):** Altere o plano de fundo do launcher escolhendo entre imagens estáticas ou opções rotativas.
- **Posição da Dock:** Escolha se a barra de aplicativos principais fica na **Base** (padrão) ou no **Topo** da tela.
- **Ajuste Vertical:** Suba ou desça a Dock em pequenos passos para encaixar perfeitamente na sua TV (20px por clique).
- **Fontes:** Interface otimizada com fontes mais nítidas, grossas e com sombras para melhor leitura a distância.

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
