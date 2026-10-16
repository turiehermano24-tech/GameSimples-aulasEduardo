# Cannon Game

Jogo Android desenvolvido para a disciplina de **Programação III**, com base na proposta do **Capítulo 6 — Cannon Game App**, do livro *Android 6 for Programmers: An App-Driven Approach*, 3ª edição.

O jogador controla um canhão e precisa destruir nove alvos em movimento antes que o tempo termine, evitando um bloqueador. Além do modo Clássico, o projeto oferece um modo Arcade com fases contínuas, dificuldade crescente e recordes locais.

## Integrantes

- **Vitor Hugo Moreira** — [VashCuBIT](https://github.com/VashCuBIT)
- **Turiê Hermano de Oliveira** — [turiehermano24-tech](https://github.com/turiehermano24-tech)

## Objetivo

Aplicar os conceitos de desenvolvimento Android estudados na disciplina: animação manual quadro a quadro, desenho com `Canvas` e `Paint`, reprodução de sons, eventos de toque e controle de uma thread dedicada ao loop do jogo com `SurfaceView` e `SurfaceHolder`.

O código da aplicação e dos testes é escrito em **Java**. Os layouts são definidos em XML e os arquivos de configuração do Gradle usam Groovy.

## Como jogar

1. No menu, escolha o modo **Clássico** ou **Arcade** e toque em **Jogar**.
2. Toque ou arraste na área de jogo para mirar e disparar. Apenas uma bola pode permanecer na tela por vez.
3. Destrua os nove alvos antes de o relógio chegar a zero. A partida começa com **10 segundos**; cada alvo atingido acrescenta **3 segundos**, e atingir o bloqueador desconta **2 segundos**.
4. Acerte alvos consecutivos para aumentar a pontuação: os acertos valem de **100 a 500 pontos**, com multiplicador de até **5 vezes**. Errar interrompe a sequência.
5. Use **Pausar** para interromper a partida. A partir da pausa, é possível voltar ao menu e depois escolher **Continuar**.

### Modos de jogo

| Modo | Funcionamento |
| --- | --- |
| Clássico | Uma rodada com nove alvos. A partida termina quando todos são destruídos ou o tempo acaba. |
| Arcade | Ao concluir uma fase, o jogador pode avançar mantendo pontos e sequência de acertos. O tempo restante passa para a próxima fase com mínimo de 10 e máximo de 30 segundos. A velocidade aumenta 10% por fase, até o limite de 80% acima da inicial. |

## Funcionalidades implementadas

- Animação manual com atualização e desenho dos elementos a cada quadro.
- Canhão controlado por toque, alvos móveis e bloqueador com detecção de colisões.
- Efeitos sonoros para disparos, acertos nos alvos e colisões com o bloqueador.
- Tela cheia em modo imersivo e orientação horizontal.
- Menu com visual arcade inspirado em jogos de 8 bits e instruções de jogo.
- Música em repetição no menu e durante a partida, com volume reduzido e opção de ligar ou desligar.
- Pausa, retorno ao menu e continuação da partida.
- Tratamento do ciclo de vida do aplicativo, com pausa ao sair para segundo plano e restauração do estado em recriações da tela.
- Pontuação, combos e efeitos visuais de acerto e erro.
- Recordes locais separados por modo e registro da maior fase no Arcade.
- Tela de resultado com pontuação, precisão, melhor sequência e tempo de jogo.

O menu, a trilha sonora, os combos, os recordes e a progressão do modo Arcade complementam a proposta base da atividade.

## Como executar

### Pré-requisitos

- Android Studio compatível com o **Android Gradle Plugin 9.4.1** utilizado pelo projeto.
- Android SDK **API 37** instalado pelo SDK Manager.
- **JDK 25** para executar o Gradle, conforme a configuração da ferramenta no repositório. O código da aplicação utiliza compatibilidade com **Java 11**.
- Emulador ou aparelho com **Android 6.0 (API 23)** ou superior.
- Acesso à internet na primeira sincronização para baixar as dependências.

### Pelo Android Studio

1. Clone o repositório:

   ```bash
   git clone https://github.com/turiehermano24-tech/GameSimples-aulasEduardo.git
   ```

2. Abra a pasta `GameSimples-aulasEduardo` no Android Studio.
3. Aguarde a sincronização do Gradle e instale os componentes do SDK solicitados. Caso necessário, confira o JDK usado pelo Gradle nas configurações do Android Studio.
4. Inicie um emulador ou conecte um aparelho com depuração USB habilitada.
5. Selecione o módulo `app` e clique em **Run**.

### Pelo terminal

Na raiz do projeto, com o SDK Android e o JDK configurados, execute no PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

O APK de depuração será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Organização do projeto

| Local | Conteúdo |
| --- | --- |
| `app/src/main/java/br/edu/uemg/gamesimples/` | Código Java do jogo, menu, áudio, pontuação e recordes. |
| `app/src/main/res/layout/` | Layouts das telas. |
| `app/src/main/res/drawable/` | Recursos visuais do menu e dos botões. |
| `app/src/main/res/raw/` | Efeitos sonoros e música. |
| `app/src/main/res/values/` | Textos, cores e estilos. |
| `app/src/test/` | Testes unitários de pontuação e progressão. |
| `app/src/androidTest/` | Testes instrumentados de continuidade da partida, menu e modo Arcade. |

`CannonView` concentra o desenho, a interação por toque e o loop do jogo. As classes `Cannon`, `Cannonball`, `Target` e `Blocker` representam os elementos da partida. `ScoreTracker` e `ArcadeRules` organizam as regras de pontuação e progressão; `GameMusic` e `GameRecords` cuidam da trilha sonora e dos recordes.

## Verificações

Para executar os testes unitários e a análise estática:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug
```

Para executar os testes instrumentados, mantenha um emulador ou aparelho conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Referências e recursos

- Proposta da disciplina de Programação III e Capítulo 6 — *Cannon Game App*, do livro *Android 6 for Programmers: An App-Driven Approach*, 3ª edição.
- Efeitos `cannon_fire.wav`, `target_hit.wav` e `blocker_hit.wav`: materiais disponibilizados pelo professor.
- Trilha sonora: arquivo `the_mountain-game-game-music-508018.mp3`, incorporado ao projeto como `menu_theme.mp3`.
