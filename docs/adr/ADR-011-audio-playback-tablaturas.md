# ADR 011 — Reprodução de áudio da tablatura: alphaTab in-browser
**Status:** Proposto
**Data:** 2026-04-28
**Responsável:** Tech Co-Founder (projeto GuitarGPT)

## Contexto
O GuitarGPT hoje gera tablaturas em ASCII via Claude API (ADR-009). Foi solicitado adicionar a capacidade de **ouvir a tablatura gerada**, com referência inicial à experiência da Suno AI ("clicar em play e ouvir um áudio").

A pergunta arquitetural é dupla:

1. **Para que a tab vire som, precisamos chamar outra IA?** A resposta importa porque uma segunda chamada de IA dobra (ou mais) o custo por geração e adiciona latência.
2. **Se decidirmos usar IA generativa de música no futuro, qual provedor / modelo / licença é viável?**

Critérios do projeto (definidos pelo usuário): **gratuito ou baixo custo**, **fácil de integrar**, **boa UX**.

Restrição importante: a tab **já existe** quando entramos nesse fluxo. Tocar uma sequência de notas conhecida é um problema **determinístico** (parser + síntese MIDI), não um problema de IA.

## Opções

### Opção A — Caminho determinístico: parser + alphaTab in-browser ✅
ASCII tab (ou AlphaTex) gerado pelo Claude → biblioteca **alphaTab** renderiza visualmente + reproduz via `alphaSynth` embutido. Zero backend de áudio, zero custo por play.

### Opção B — Caminho determinístico server-side: ASCII tab → MIDI → MP3 via FluidSynth
Parser ASCII tab → MIDI (music21 ou PyGuitarPro) → renderiza para MP3 com **FluidSynth** + SoundFont gratuito (FluidR3_GM, GeneralUser GS). Servidor envia MP3 pro cliente.

### Opção C — Suno AI via reseller API
Não há API pública oficial da Suno. Acesso só por revendedores (`sunoapi.org`, `EvoLink`, `Kie.ai`) a ~$0.10–0.15 por música, latência 30–90s. Plano oficial Suno (Premier $30/mês ≈ $0.03–0.04/música) é UI-only.

### Opção D — APIs alternativas de geração musical (Mubert / Stable Audio / Beatoven / AIVA / Loudly)
Provedores com tiers gratuitos limitados ou pagos.

### Opção E — Self-host de modelo open-source (MusicGen / AudioCraft / Riffusion)
Rodar inferência própria em GPU AWS.

## Análise Comparativa

| Critério | A: alphaTab | B: FluidSynth server | C: Suno reseller | D: Mubert et al. | E: Self-host MusicGen |
|---|---|---|---|---|---|
| Custo por execução | **Zero** | Zero (CPU) | $0.10–0.15 | $50–200/mês fixo | $0.003–0.006 (com GPU quente) |
| Custo de infra fixo | Zero | Zero (server já existe) | Zero | Zero | ~$1/h (g5.xlarge AWS) |
| Latência | <1s | 1–3s | 30–90s | 5–30s | 10–20s |
| Toca **a tab gerada**? | ✅ Sim | ✅ Sim | ❌ Gera música nova | ❌ Gera música nova | ❌ Gera música nova |
| Setup | ✅ 1 lib JS (`alphaTab` MPL-2.0) | Médio (FluidSynth + SF2 + parser) | Cadastro + API key | Cadastro + API key | GPU + container + modelo |
| Licença para uso comercial | ✅ MPL-2.0 (alphaTab) + permissivo (SoundFonts) | ✅ LGPL (FluidSynth) + permissivo | ⚠️ TOS do reseller, não da Suno | Variável | ❌ **CC-BY-NC** (proibido comercial) |
| UX visual + áudio integrados | ✅ Sim (mesma lib renderiza tab e toca) | Não (precisa player separado) | Só áudio | Só áudio | Só áudio |
| Realismo do som | Médio (synth GM) | Médio (depende do SF2) | Alto | Alto | Alto |
| Risco regulatório | Nenhum | Nenhum | Médio (TOS reseller) | Baixo-médio | **Alto** (licença NC) |
| Quanto resolve o problema declarado | ✅ 100% | ✅ 100% | 0% (escopo errado) | 0% (escopo errado) | 0% (escopo errado) |

## Decisão proposta
**Adotar Opção A (alphaTab in-browser)** como solução do problema "tocar a tab gerada". Postergar Suno (Opção C) atrás de feature flag para quando/se houver demanda por "transformar este riff em uma música completa". Descartar B, D, E.

## Justificativa

### 1. O problema declarado é determinístico, não generativo
"Quero ouvir esta tab específica" não é um problema de IA. As notas, ritmo, técnica e duração já estão na string. Chamar uma segunda IA para sintetizar áudio é arquitetural e financeiramente errado:

- Suno não recebe "toque exatamente estas notas" — ela gera **música nova** a partir de prompt textual. Você não consegue garantir que o áudio reproduza o riff que o Claude escreveu.
- O mesmo vale para Mubert, Stable Audio, MusicGen — todos geram música a partir de prompt, não tocam input MIDI/tab.

A ferramenta certa para "tocar isso aqui" é um **synth MIDI**, não uma IA musical.

### 2. alphaTab resolve render + play numa biblioteca
**alphaTab** (MPL-2.0, ativa desde 2010) parseia AlphaTex / Guitar Pro / MusicXML, renderiza tab + pentagrama no canvas/SVG e toca via **alphaSynth** (synth MIDI WebAudio embutido). Uma única dependência no frontend cobre o problema inteiro.

Ajuste necessário no backend: refinar o system prompt do `ClaudeTablatureGenerator` para emitir tab num formato que o alphaTab parseie (AlphaTex é o caminho mais natural — sintaxe próxima a ASCII tab, mas estruturada). Custo: editar uma string. Não exige novo serviço, novo banco, nova fila.

### 3. Custo por play = zero
Síntese acontece no browser do usuário. Não há requisição ao backend para tocar. Não há cota a vigiar, não há billing surpresa.

### 4. Por que NÃO Suno (Opção C) hoje
- **Sem API oficial em 2026** — só resellers com TOS frágeis e SLA fraca.
- **Custo**: $0.10–0.15/música pelos resellers. Para um projeto pessoal isso é quase 2x o custo da geração de tab pelo Claude (~$0.08).
- **Latência**: 30–90s adiciona ao 18s do Claude → minuto+ até o usuário ouvir algo.
- **Não toca a tab gerada** — gera música paralela. UX inconsistente ("a tab diz X, o áudio toca Y").
- **Faz sentido como feature separada** ("inspire uma música a partir deste riff") — registrar como flag futuro, não como solução de playback.

### 5. Por que NÃO MusicGen self-hosted (Opção E)
- **Pesos sob CC-BY-NC 4.0** — proibido qualquer uso comercial. Mata SaaS futuro.
- **Custo de GPU**: `g5.xlarge` na AWS (~$1/h). Mesmo com GPU quente, manter ela ligada durante períodos ociosos custa mais que pagar por geração on-demand em qualquer outro provedor.
- Não toca a tab — mesmo problema da Opção C.

### 6. Por que NÃO server-side FluidSynth (Opção B) hoje
Funciona, mas:
- Adiciona dependência nativa no servidor (FluidSynth compilado).
- Adiciona pipeline: parser ASCII → MIDI → render MP3 → upload para storage (S3?) → URL para o cliente.
- **alphaTab faz a mesma coisa no cliente**, sem servidor envolvido.
- Vale reconsiderar SE precisarmos de áudio "definitivo" para download offline (export MP3) — aí a renderização server-side faz sentido.

## Pré-requisitos da Opção A

1. **Frontend existir** (hoje `frontend/` está vazio) — ver BACKLOG.
2. **Refinar system prompt do Claude** para emitir AlphaTex em vez de ASCII tab livre. AlphaTex tem sintaxe estruturada (compassos, notas, técnicas) que o alphaTab parseia diretamente.
3. **Validar qualidade** do AlphaTex emitido pelo Claude com 5–10 prompts variados antes de mergear o frontend.
4. **Salvar tanto AlphaTex quanto explicação didática** no `result_text` (estrutura: JSON com campos `alphatex` + `explanation`, ou separadores claros).

## Estratégia de evolução por tiers

| Tier | Escopo | Quando |
|---|---|---|
| **Tier 0** (hoje) | Tab em ASCII puro, sem áudio | Estado atual |
| **Tier 1** | alphaTab no browser: tab visual + play MIDI | Próxima iteração de frontend |
| **Tier 2** | Export MIDI/MP3 server-side (FluidSynth + SoundFont) | Quando usuário quiser baixar áudio offline |
| **Tier 3** | Feature opcional "remix como música completa" via Suno reseller | Quando houver demanda explícita; atrás de feature flag e gating de custo |

## Consequências

- **Positivas**:
  - Resolve a UX de "ouvir o que foi gerado" sem custo recorrente.
  - Render visual da tab (pentagrama + tab) vem de brinde com a mesma lib.
  - Mantém arquitetura hexagonal: nada muda no backend além do system prompt.
  - Caminho claro para Tier 2 (export server-side) se necessário.

- **Negativas**:
  - Som de synth MIDI (`alphaSynth` usa SoundFont embutido) é "OK", não "estúdio". Para personal project é suficiente; para produto pago talvez não.
  - Exige que o frontend exista — bloqueia em outra iniciativa.
  - Refinar o system prompt para emitir AlphaTex consistente exige iteração e validação de qualidade.

- **Reversão**: trivial. Se alphaTab não atender, trocar por Tone.js + WebAudioFont, ou ir para Tier 2 (server-side). Nenhuma decisão de banco ou domínio é afetada.

## Referências consultadas
- alphaTab: https://alphatab.net/
- alphaTab GitHub: https://github.com/CoderLine/alphaTab
- WebAudioFont: https://github.com/surikov/webaudiofont
- FluidSynth: https://www.fluidsynth.org/
- SoundFont GeneralUser GS: https://schristiancollins.com/generaluser.php
- Suno pricing: https://suno.com/pricing
- AudioCraft (MusicGen): https://github.com/facebookresearch/audiocraft
- Stability AI pricing: https://platform.stability.ai/pricing
- Mubert API: https://landing.mubert.com/
- AIVA legal: https://www.aiva.ai/legal/1
