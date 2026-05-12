# GuitarGPT — Backlog de Evoluções Futuras

Itens identificados durante a construção, **fora do escopo da iteração atual**. Cada um tem critério de "quando puxar" e link para artefato de decisão (ADR), quando aplicável.

> Para decisões já tomadas, ver `docs/adr/`. Este arquivo é apenas a fila de "próximos candidatos".

---

## 🎵 Renderização e formatação da tablatura
**Origem:** observado em 2026-04-28 — Swagger UI mostra `result_text` como JSON inline, sem fonte monoespaçada nem alinhamento de colunas, então a tab "parece torta". O conteúdo no banco está correto; o problema é puramente de apresentação.

**Necessário:**
- Frontend dedicado que renderize a tab em fonte monoespaçada (mínimo viável: `<pre>` com CSS).
- Idealmente: parser de ASCII tab → renderização visual (notas em pentagrama + tab) usando biblioteca como **alphaTab** (ver ADR-011).

**Quando puxar:** junto com a primeira iteração de `frontend/` (atualmente vazio).

---

## 🔊 Geração de áudio a partir da tablatura
**Origem:** pedido de UX em 2026-04-28 — usuário quer ouvir a tab gerada (referência: experiência tipo "Suno AI").

**Resumo da viabilidade** (análise completa em [ADR-011](adr/ADR-011-audio-playback-tablaturas.md), status `Proposto`):
- **Caminho recomendado**: ASCII tab → AlphaTex/MIDI → tocar no browser via **alphaTab + alphaSynth** (zero custo por play, MPL-2.0).
- **Suno API**: não há API pública oficial; só revendedores (~$0.10–0.15/música) — postergar para iteração futura, atrás de feature flag.
- **MusicGen / AudioCraft (Meta)**: descartado — pesos sob CC-BY-NC (proibido uso comercial) + custo GPU.
- **Mubert / AIVA / Beatoven / Loudly**: descartado — geram música nova, não tocam a tab existente. Ferramenta errada.

**Quando puxar:** após o frontend mínimo. Tier 1 (alphaTab in-browser) é o MVP.

---

## 🧩 Composição prompt template + variáveis
**Origem:** registrado em ADR-009 — hoje o `template_id` na request é só metadado, a chamada para Claude usa só o `userPrompt`.

**Necessário:** ler o `PromptTemplate` ativo do banco, parsear placeholders (`{key}`, `{scale}`, `{tempo}`...) e compor o prompt final para o Claude.

**Quando puxar:** quando os usuários começarem a usar templates de fato (UX que mostre os campos do template e peça os valores).

---

## 📊 Observabilidade — instrumentar métricas custom
**Origem:** ADR-010 (`Aprovado`).

**Necessário:** adicionar dependência `micrometer-registry-prometheus`, expor `/actuator/prometheus`, instrumentar 4 métricas no `ClaudeTablatureGenerator` (`request.duration`, `tokens`, `cost.usd`, `queue.lag`), configurar Grafana Alloy + Grafana Cloud.

**Quando puxar:** próxima iteração técnica. Bloqueio: sem isso, custo da Claude é invisível em produção.

---

## 🎚️ Streaming de respostas Claude
**Origem:** ADR-009 (registrado como evolução futura).

**Necessário:** trocar `client.messages().create()` por `client.messages().createStreaming()`, propagar chunks via WebSocket/SSE, atualizar `result_text` incrementalmente.

**Quando puxar:** quando uma geração começar a estourar `max_tokens=8000` OU se a UX exigir feedback "ao vivo".

---

## 💾 Cache de prompt Claude (`cache_control`)
**Origem:** ADR-009.

**Necessário:** adicionar `cache_control` no system prompt do `ClaudeTablatureGenerator`. Ativa quando o prompt passa de 4096 tokens — o que não acontece hoje, mas vai acontecer quando incluirmos exemplos few-shot.

**Quando puxar:** junto com expansão do system prompt (adição de exemplos, regras de teoria musical mais ricas, multi-idioma).

---

## 🧠 Extended thinking
**Origem:** ADR-009.

**Necessário:** habilitar `thinking` + `effort: high` na chamada Claude para pedidos complexos (composições longas, restrições musicais múltiplas).

**Quando puxar:** após primeira rodada de feedback de qualidade. Trade-off: mais qualidade, mais latência, mais custo.
