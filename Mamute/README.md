
# Bamco_f (local workspace)

Projeto Java/Maven contendo dois subprojetos:

- `mavenproject6/` — exemplo Maven mínimo
- `webserve/` — servidor web (Firebase + arquivos estáticos em `src/main/resources/public`)

Visão geral do repositório

- Estrutura principal: `mavenproject6/`, `webserve/`, `README.md`, `.gitignore`, `publish_to_github.ps1`.
- Os arquivos de configuração do Firebase (`firebase-config.json`, `google-services.json`) estão presentes em `webserve/src/main/resources` — trate-os como segredos se for publicar em repositório público.

Segurança e segredos

- Remova ou substitua credenciais antes de publicar publicamente. Recomendo manter chaves/segredos fora do repositório e usar variáveis de ambiente ou um cofre de segredos.

Requisitos de ambiente

- Java 11+ (JDK)
- Maven
- Git (para publicar no GitHub)

Build e execução (local)

1. Instale o Maven e o JDK (Java 11+ recomendado).
2. Para compilar o projeto `webserve`:

```powershell
cd /d D:\Mamute\webserve
mvn clean package
```

3. Para executar (exemplo):

```powershell
cd /d D:\Mamute\webserve
java -jar target\webserve-1.0-SNAPSHOT.jar
```

Publicar no GitHub — script automatizado

Um script PowerShell `publish_to_github.ps1` foi adicionado ao diretório raiz. Ele automatiza os passos para inicializar o repositório (se necessário), adicionar o remoto `origin`, criar um commit (cria um commit vazio se não houver alterações) e dar um `git push --force` para `origin/main`.

Uso do script:

```powershell
cd /d D:\Mamute
powershell -ExecutionPolicy Bypass -File .\publish_to_github.ps1
```

Autenticação GitHub

- Durante o push o Git solicitará autenticação. Recomendo usar um Personal Access Token (PAT) do GitHub com escopo `repo` (use o token como senha quando o Git pedir). Crie-o em: https://github.com/settings/tokens
- Se preferir não forçar (`--force`) o histórico remoto, substitua o `--force` por `--force-with-lease` no script.

Checklist antes de publicar

- [ ] Remover/rotular quaisquer arquivos sensíveis (ex.: `firebase-config.json`) ou movê-los para um cofre.
- [ ] Verificar `.gitignore` para garantir que artefatos de build e uploads não sejam versionados.
- [ ] Atualizar `README.md` com instruções de uso/instalação específicas do projeto.

O que eu posso fazer por você

- Gerar documentação adicional (`CONTRIBUTING.md`, `CHANGELOG.md`) — já adicionei um `CHANGELOG.md` (veja no repositório).
- Aguardar você executar o script localmente e então verificar o resultado.
- Quando o `git` estiver disponível neste ambiente, eu posso executar os passos here (se você permitir).

