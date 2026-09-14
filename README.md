# Almoxarifado API

API em Java (Spring Boot) + MySQL para o [Almoxarifado em Agenda](https://github.com/brunotaveiradasilva/almoxarifado):
guarda materiais e agendamentos num banco de verdade, para acessar os mesmos dados de qualquer computador —
não só do navegador onde foram cadastrados.

## Stack

- Java 21
- Spring Boot 3 (Web, Data JPA, Validation, Actuator)
- MySQL 8
- Docker / docker compose para rodar local e fazer deploy

## Rodando local

### Opção 1 — Docker (não precisa instalar Java nem MySQL)

```bash
docker compose up --build
```

Sobe o MySQL e a API juntos. A API fica em `http://localhost:8080`.

### Opção 2 — Java + Maven na máquina

Precisa de JDK 21 e Maven instalados, e um MySQL rodando (pode ser o do `docker compose up mysql`
sozinho). Copie `.env.example` para `.env`, ajuste se precisar, exporte as variáveis e rode:

```bash
mvn spring-boot:run
```

## Endpoints

Todos sob o prefixo `/api`. Corpos e respostas em JSON, no mesmo formato usado pelo front-end
(`types.ts`).

| Método | Rota                        | Descrição                                   |
|--------|------------------------------|----------------------------------------------|
| GET    | `/api/materiais`             | Lista todos os materiais                      |
| POST   | `/api/materiais`             | Cria um material                              |
| PUT    | `/api/materiais/{id}`        | Atualiza um material                          |
| DELETE | `/api/materiais/{id}`        | Exclui um material                            |
| GET    | `/api/agendamentos`          | Lista todos os agendamentos                   |
| POST   | `/api/agendamentos`          | Cria um agendamento                           |
| PUT    | `/api/agendamentos/{id}`     | Atualiza um agendamento                       |
| PATCH  | `/api/agendamentos/{id}/status` | Só troca o status (`{"status": "retirado"}`) |
| DELETE | `/api/agendamentos/{id}`     | Exclui um agendamento                         |
| GET    | `/actuator/health`           | Health check (usado pelo Railway/Render)      |

## Variáveis de ambiente

| Variável                | Padrão (local)                              | Para que serve                         |
|--------------------------|----------------------------------------------|------------------------------------------|
| `PORT`                   | `8080`                                        | Porta HTTP da API                        |
| `DB_URL`                 | `jdbc:mysql://localhost:3306/almoxarifado`    | URL JDBC do MySQL                        |
| `DB_USERNAME`             | `almoxarifado`                               | Usuário do banco                         |
| `DB_PASSWORD`             | `almoxarifado`                               | Senha do banco                           |
| `CORS_ALLOWED_ORIGINS`   | `http://localhost:5173,https://brunotaveiradasilva.github.io` | Origens que podem chamar a API, separadas por vírgula |

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
   - (o Railway já define `PORT` sozinho — não precisa mexer)
5. Gere um domínio público em **Settings → Networking → Generate Domain**. Essa URL
   (`https://algo.up.railway.app`) é o `VITE_API_URL` que o front-end vai usar.

Render funciona de forma parecida: **New → Web Service** apontando pro `Dockerfile`, e
**New → PostgreSQL/MySQL** para o banco (no plano free do Render o MySQL gerenciado não está
disponível — nesse caso, um banco MySQL gratuito externo como o do
[Railway](https://railway.app) ou [Aiven](https://aiven.io) resolve).

## Próximos passos possíveis

- Trocar `ddl-auto: update` por migrations versionadas (Flyway), quando o schema começar a mudar
  bastante.
- Autenticação (hoje a API é aberta para qualquer um com a URL).
