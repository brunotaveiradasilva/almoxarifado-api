# Almoxarifado API

API em Java (Spring Boot) + MySQL para o [Almoxarifado em Agenda](https://github.com/brunotaveiradasilva/almoxarifado):
guarda materiais e agendamentos num banco de verdade, para acessar os mesmos dados de qualquer computador —
não só do navegador onde foram cadastrados.

## Stack

- Java 21
- Spring Boot 3 (Web, Data JPA, Validation, Actuator, Security)
- MySQL 8
- Login com token JWT (implementação própria, sem lib externa — ver `auth/JwtService.java`)
- Docker / docker compose para rodar local e fazer deploy

## Rodando local

### Opção 1 — Docker (não precisa instalar Java nem MySQL)

```bash
docker compose up --build
```

Sobe o MySQL e a API juntos. A API fica em `http://localhost:8080`, já com um login
`admin` / `admin123` criado sozinho na primeira vez que sobe (ver seção **Login** abaixo).

### Opção 2 — Java + Maven na máquina

Precisa de JDK 21 e Maven instalados, e um MySQL rodando (pode ser o do `docker compose up mysql`
sozinho). Copie `.env.example` para `.env`, ajuste se precisar, exporte as variáveis e rode:

```bash
mvn spring-boot:run
```

## Login

A API inteira exige login — só `POST /api/auth/login` fica aberto, o resto sempre precisa de um
token válido no header `Authorization: Bearer <token>`.

Na primeira vez que a API sobe sem nenhum usuário cadastrado, ela cria um login sozinha a partir
de `ADMIN_USERNAME` / `ADMIN_PASSWORD`. Se `ADMIN_PASSWORD` não estiver definida, gera uma senha
aleatória e mostra ela **uma única vez** no log de inicialização (procure por `Nenhum usuario
existia ainda` nos logs do Railway/`docker compose logs`).

```bash
# entrar
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","senha":"admin123"}'
# -> {"token":"...","usuario":"admin","role":"ADMIN"}

# criar outro login (precisa estar autenticado como ADMIN)
curl -X POST http://localhost:8080/api/auth/usuarios \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"usuario":"maria","senha":"uma-senha-forte"}'
```

Não existe endpoint de auto-cadastro público de propósito: só quem já tem login pode criar outro.

Todo login tem um papel (`role`): `ADMIN` ou `USUARIO`. O login criado pelo bootstrap
(`ADMIN_USERNAME`) sempre vira `ADMIN`; qualquer login criado depois nasce `USUARIO`. Só `ADMIN`
consegue criar/excluir outros logins e gerenciar representantes e tipos de meta — o resto da API
(materiais, agendamentos, trocar a própria senha) continua liberado pra qualquer login autenticado.

Dentro do app, tudo isso (listar, criar, excluir logins e trocar a própria senha) já tem tela —
não precisa usar `curl` no dia a dia, é só pra quando ninguém consegue mais entrar (próxima seção).

### Recuperando o acesso

Se ninguém souber a senha do `admin` (ex: foi a gerada automaticamente e o log já rolou), defina
no serviço:

```
RESET_ADMIN_PASSWORD=true
ADMIN_PASSWORD=uma-senha-nova-que-voce-escolheu
```

e reinicie/redeploy a API. No próximo boot ela redefine a senha desse login para o valor de
`ADMIN_PASSWORD`, mesmo que ele já exista. **Depois de entrar, apague ou volte `RESET_ADMIN_PASSWORD`
para `false`** — senão a senha é redefinida de novo a cada restart.

## Endpoints

Todos sob o prefixo `/api`. Corpos e respostas em JSON, no mesmo formato usado pelo front-end
(`types.ts`). Todos exigem login (seção acima), exceto `/api/auth/login`.

| Método | Rota                        | Descrição                                   |
|--------|------------------------------|----------------------------------------------|
| POST   | `/api/auth/login`            | Login — devolve o token JWT                   |
| GET    | `/api/auth/usuarios`         | Lista os nomes de usuário cadastrados         |
| POST   | `/api/auth/usuarios`         | Cria outro login (exige ser ADMIN)            |
| DELETE | `/api/auth/usuarios/{usuario}` | Exclui um login (exige ser ADMIN; nunca o último que resta) |
| PATCH  | `/api/auth/senha`            | Troca a própria senha (`{"senhaAtual","novaSenha"}`) |
| PATCH  | `/api/auth/avatar`           | Troca a própria foto de perfil (`{"avatar"}`, data URL base64; manda vazio/nulo pra remover) |
| GET    | `/api/materiais`             | Lista todos os materiais                      |
| POST   | `/api/materiais`             | Cria um material                              |
| PUT    | `/api/materiais/{id}`        | Atualiza um material                          |
| DELETE | `/api/materiais/{id}`        | Exclui um material                            |
| GET    | `/api/agendamentos`          | Lista todos os agendamentos                   |
| POST   | `/api/agendamentos`          | Cria um agendamento                           |
| PUT    | `/api/agendamentos/{id}`     | Atualiza um agendamento                       |
| PATCH  | `/api/agendamentos/{id}/status` | Só troca o status (`{"status": "retirado"}`) |
| DELETE | `/api/agendamentos/{id}`     | Exclui um agendamento                         |
| GET    | `/api/fornecedores`          | Lista os fornecedores (exige ser ADMIN)       |
| POST   | `/api/fornecedores`          | Cria um fornecedor (exige ser ADMIN)          |
| PUT    | `/api/fornecedores/{id}`     | Atualiza um fornecedor (exige ser ADMIN)      |
| DELETE | `/api/fornecedores/{id}`     | Exclui um fornecedor (exige ser ADMIN; recusa se ele tiver metas) |
| GET    | `/api/representantes`            | Lista os representantes, com os fornecedores de cada um (exige ser ADMIN) |
| POST   | `/api/representantes`            | Cria um representante (`{"nome","fornecedorIds","email","celular","codigoAds"}`, `codigoAds` é opcional — ver **Integração com a ADS** — exige ser ADMIN) |
| PUT    | `/api/representantes/{id}`       | Atualiza um representante (exige ser ADMIN)        |
| DELETE | `/api/representantes/{id}`       | Exclui um representante (exige ser ADMIN)          |
| GET    | `/api/metas`                 | Lista as metas, com o fornecedor de cada uma (exige ser ADMIN) |
| POST   | `/api/metas`                 | Cria uma meta (`{"nome","fornecedorId","unidade","codigoAdsDivisao","cnpjAdsFornecedor"}`, `unidade` é `KG`, `UNIDADE` ou `REAL`; os dois últimos são opcionais — ver **Integração com a ADS** — exige ser ADMIN) |
| PUT    | `/api/metas/{id}`            | Atualiza uma meta (exige ser ADMIN)           |
| DELETE | `/api/metas/{id}`            | Exclui uma meta (exige ser ADMIN)             |
| GET    | `/api/metas-representante`        | Lista os valores de meta atribuídos aos representantes (exige ser ADMIN) |
| POST   | `/api/metas-representante/sincronizar` | Força agora o recálculo do realizado a partir da ADS (exige ser ADMIN) |
| POST   | `/api/metas-representante`        | Atribui um valor de meta a um representante (`{"representanteId","metaId","valorMeta","valorRealizado"}`, exige ser ADMIN) |
| PUT    | `/api/metas-representante/{id}`   | Atualiza um valor de meta (exige ser ADMIN)   |
| DELETE | `/api/metas-representante/{id}`   | Exclui um valor de meta (exige ser ADMIN)     |
| GET    | `/actuator/health`           | Health check (usado pelo Railway/Render), sem login |

## Variáveis de ambiente

| Variável                | Padrão (local)                              | Para que serve                         |
|--------------------------|----------------------------------------------|------------------------------------------|
| `PORT`                   | `8080`                                        | Porta HTTP da API                        |
| `DB_URL`                 | `jdbc:mysql://localhost:3306/almoxarifado`    | URL JDBC do MySQL                        |
| `DB_USERNAME`             | `almoxarifado`                               | Usuário do banco                         |
| `DB_PASSWORD`             | `almoxarifado`                               | Senha do banco                           |
| `CORS_ALLOWED_ORIGINS`   | `http://localhost:5173,https://brunotaveiradasilva.github.io` | Origens que podem chamar a API, separadas por vírgula |
| `JWT_SECRET`             | chave de desenvolvimento (fraca, só local)   | Assina os tokens — **troque por um valor forte e único antes de publicar** (ex: `openssl rand -base64 48`) |
| `JWT_VALIDADE_HORAS`     | `168` (7 dias)                                | Por quanto tempo um login fica valendo sem precisar entrar de novo |
| `ADMIN_USERNAME`         | `admin`                                       | Nome do primeiro login, criado sozinho se o banco não tiver nenhum usuário |
| `ADMIN_PASSWORD`         | *(gera uma aleatória e loga se não definir)*  | Senha do primeiro login                  |
| `RESET_ADMIN_PASSWORD`   | `false`                                       | `true` força redefinir a senha de `ADMIN_USERNAME` no próximo boot, mesmo que já exista — ver **Recuperando o acesso** |
| `ADS_API_URL`            | `https://adsapi.com.br`                       | URL base da API da ADS (histórico de vendas) — produção, não homologação |
| `ADS_API_KEY`            | *(vazio)*                                     | Header `x-api-key` da API da ADS — ver **Integração com a ADS** |
| `ADS_USER_AGENT`         | `SulBiologic`                                 | Header `User-Agent` exigido pela API da ADS |
| `ADS_ESPECIFICO_ID`      | `074`                                          | Valor fixo da conta, exigido em toda chamada de histórico de vendas (não é um filtro) |
| `ADS_CNPJ_DISTRIBUIDORA` | *(vazio)*                                    | CNPJ da distribuidora, usado como path param nas chamadas à ADS |

## Integração com a ADS (histórico de vendas)

O `valorRealizado` das metas (`/api/metas-representante`) é calculado sozinho a partir do
histórico de vendas da [API da ADS](https://adsapi.com.br/api/v1) — não precisa mais editar esse
valor na mão (dá pra editar direto se quiser, mas o job de sincronização sobrescreve de novo).

**Autenticação:** confirmada testando direto contra a API de produção — só os headers
`x-api-key` e `User-Agent`, sem login nem Bearer token (a doc/spec da ADS não documenta isso; o
ambiente de homologação, `hom.adsapi.com.br`, nem aceita essa autenticação — use sempre
`adsapi.com.br`, produção).

**`especificoid`:** é um valor fixo da conta (`ADS_ESPECIFICO_ID`, ex: `074`), não um filtro —
testado com outros valores e todos deram `400`. Vai sempre igual em toda chamada.

**Como cada representante/meta se liga à ADS:**

- Cada **Representante** tem um campo opcional `codigoAds`, preenchido na tela de cadastro —
  é o `codigo` dele na ADS (`GET /api/v1/{cnpj}/representantes`), usado como `repr_id` na consulta
  de histórico de vendas.
- Cada **Meta** tem dois campos opcionais, preenchidos na tela de cadastro — só um dos dois é
  usado por meta (`cnpjAdsFornecedor` tem prioridade se os dois estiverem preenchidos):
  - `codigoAdsDivisao`: código (ou vários, separados por vírgula, ex: `112,113`) da divisão
    correspondente na ADS (`GET /api/v1/{cnpj}/divisoes`) — soma só os itens vendidos nessa(s)
    divisão(ões). Pra metas específicas de uma linha de produto (ex: "Cookie", "Umidos").
  - `cnpjAdsFornecedor`: CNPJ do fornecedor na ADS (`GET /api/v1/{cnpj}/fornecedores`, campo
    `cnpjCpf`) — soma tudo vendido desse fornecedor, sem filtrar por divisão. Pra metas
    "catch-all" tipo "Geral", que somam o fornecedor inteiro.
- Representante ou meta sem nenhum desses campos preenchidos simplesmente não são sincronizados
  (o resto do app funciona igual, com edição manual do `valorRealizado`).

**Cálculo:** `AdsSincronizacaoService` busca, uma vez por representante (com `codigoAds`
preenchido), todo o histórico de vendas do mês corrente (dia 1 até hoje) filtrado por `repr_id`.
Pra cada meta atribuída a esse representante, soma por `cnpjAdsFornecedor` (toda venda desse
fornecedor) ou por `codigoAdsDivisao` (só os itens cuja `divisao.id` bate com um dos códigos):

| Unidade da meta | Campo somado |
|---|---|
| `REAL` | `itens[].valores.valorProduto` |
| `KG` | `itens[].peso.bruto` |
| `UNIDADE` | `itens[].quantidade` |

**Quando roda:** todo dia às 6h (`@Scheduled` em `AdsSincronizacaoService`), recalculando o mês
inteiro do zero (não é incremental). Também dá pra forçar na hora: `POST
/api/metas-representante/sincronizar` (exige ser ADMIN, mesma resposta de `GET
/api/metas-representante`).

Se a ADS estiver fora do ar ou recusar a chamada pra um representante, esse representante fica de
fora do recálculo daquela vez (loga um aviso) — os outros continuam normalmente.

## Deploy (Railway)

1. Crie uma conta em [railway.app](https://railway.app) (dá para logar com a conta do GitHub).
2. **New Project → Deploy from GitHub repo** e escolha `almoxarifado-api`. O Railway detecta o
   `Dockerfile` sozinho e builda a partir dele.
3. **New → Database → Add MySQL** no mesmo projeto. O Railway cria as variáveis `MYSQLHOST`,
   `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD` automaticamente.
4. No serviço da API, aba **Variables**, defina:
   - `DB_URL` = `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}`
   - `DB_USERNAME` = `${{MySQL.MYSQLUSER}}`
   - `DB_PASSWORD` = `${{MySQL.MYSQLPASSWORD}}`
   - `CORS_ALLOWED_ORIGINS` = `https://brunotaveiradasilva.github.io`
   - `JWT_SECRET` = um valor aleatório forte (gere com `openssl rand -base64 48`, por exemplo)
   - `ADMIN_USERNAME` e `ADMIN_PASSWORD` = o login que você vai usar pra entrar no app
   - (o Railway já define `PORT` sozinho — não precisa mexer)
5. Gere um domínio público em **Settings → Networking → Generate Domain**. Essa URL
   (`https://algo.up.railway.app`) é o `VITE_API_URL` que o front-end vai usar.

Render funciona de forma parecida: **New → Web Service** apontando pro `Dockerfile`, e
**New → PostgreSQL/MySQL** para o banco (no plano free do Render o MySQL gerenciado não está
disponível — nesse caso, um banco MySQL gratuito externo como o do
[Railway](https://railway.app) ou [Aiven](https://aiven.io) resolve).

## Próximos passos possíveis

- Trocar `ddl-auto: update` por migrations versionadas (Flyway), quando o schema começar a mudar
  bastante. Sem isso, `ddl-auto: update` só adiciona colunas/tabelas, nunca remove: se você já
  tinha rodado a API com a versão antiga de `Representante` (com o campo `codigo`) ou com a tabela
  `tipos_meta`, apague a coluna `codigo` de `representantes` e a tabela `tipos_meta` manualmente antes
  de subir esta versão.
- Endpoint de logout/revogação — hoje um token vale até expirar (`JWT_VALIDADE_HORAS`), não tem
  como invalidar um antes da hora.
- Permitir escolher o `role` do login ao criar outro usuário pela tela (hoje todo login criado
  pela tela/API nasce `USUARIO`; promover a `ADMIN` só é possível direto no banco).
