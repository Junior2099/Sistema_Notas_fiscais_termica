# Papelaria

## Atualização 0.5 — PDF para Knup KP-1025

- Texto do comprovante PDF reforçado em negrito, inclusive CNPJ e rodapé.
- Área de conteúdo de **48 mm**, com margens de **5 mm** nas laterais, conforme a largura efetiva informada pela Knup para a KP-1025.
- A página continua fixa em **58 × 210 mm**, orientação retrato.
- A rotina de impressão direta foi mantida.

Gere novamente o PDF pelo histórico; os arquivos exportados anteriormente não mudam. Imprima em tamanho real / 100%. Se houver falhas no leitor, teste a opção Imprimir como imagem do Adobe Reader. O resultado físico precisa ser confirmado na impressora; os testes realizados verificaram o PDF, seus limites e conteúdo.

Referência do fabricante: https://knup.com.br/produto/kp-1025/

## Atualização 0.4 — comprovante Imprimir

- Página fixa de **58 × 210 mm**, em orientação retrato, mesmo com um único serviço.
- Cabeçalho com ícone de impressora, **Imprimir** e **CNPJ: 40.203.293/0001-29**, repetido em todas as páginas.
- Exportação PDF com rotação zero e preferência de impressão sem escala.
- Impressão direta com papel solicitado de 58 × 210 mm e orientação retrato.

Após atualizar, gere novamente o comprovante pelo botão **Salvar PDF 58 × 210**. Os PDFs exportados anteriormente mantêm o tamanho antigo. No leitor PDF e no driver, configure papel **58 × 210 mm**, orientação **Retrato**, tamanho real **100%** e desative a rotação automática. Essas configurações do leitor/driver podem substituir as preferências do PDF.

Aplicativo desktop em Java para Windows e Linux. **Versão 0.5.**

App local para cadastros de serviços de impressão, produtos de papelaria, clientes, atendimentos e arquivo das NFS-e emitidas no Emissor Nacional.

## Funcionalidades

- Cadastro e edição de serviços, produtos e clientes.
- Busca por nome, código, documento, contato e dados do histórico.
- Leitura de códigos de barras cadastrados, usando leitor em modo teclado.
- Atendimento com quantidades, valores, totais e observações dos serviços.
- Histórico que conserva preços e descrições dos atendimentos já salvos.
- Preparação da descrição dos serviços para copiar ao Emissor Nacional.
- Arquivamento, cópia e abertura do PDF oficial da NFS-e emitida no portal.
- Impressão e reimpressão de comprovante não fiscal em bobina de 58 × 210 mm.
- Exportação direta do comprovante não fiscal em PDF de 58 × 210 mm.

> A NFS-e é emitida pelo usuário no portal oficial. O aplicativo organiza os atendimentos e os documentos; nesta versão, não emite notas fiscais automaticamente.

## Requisitos

| Componente | Requisito |
| --- | --- |
| Java | Versão 17 ou superior, com suporte à interface gráfica |
| Sistema | Windows ou Linux com sessão gráfica |
| Navegador | Para acesso ao Emissor Nacional |
| Leitor de PDF | Para abrir documentos e imprimir PDFs |
| Impressora | Opcional; deve estar instalada no sistema para impressão física |
| Leitor de código de barras | Opcional; modo teclado USB com Enter |
| Internet | Necessária para usar o portal de emissão; cadastros e comprovante PDF são locais |

## Arquivos do pacote

| Arquivo ou pasta | Finalidade |
| --- | --- |
| `papelaria.jar` | Aplicativo compilado |
| `INICIAR.bat` | Inicialização no Windows |
| `INICIAR.sh` | Inicialização no Linux |
| `COMPILAR.bat` | Compilação do código no Windows |
| `TESTAR.bat` | Execução dos testes no Windows |
| `src/br/com/papelaria/` | Código-fonte Java |
| `LEIA-ME.md` | Instruções incluídas na distribuição |
| `PREVIA.png` | Imagem demonstrativa da interface |
| `EXEMPLO-Comprovante.pdf` | Comprovante não fiscal de exemplo |

## Novidades da versão 0.5

- Botão **Salvar PDF 58 × 210** no histórico: gera diretamente o comprovante não fiscal para guardar e imprimir depois, sem precisar de impressora virtual.
- Inicialização em Linux com `INICIAR.sh` ou `java -jar papelaria.jar`.
- Abertura de navegador, PDF e pasta de dados com alternativa `xdg-open` nos desktops Linux.
- Referências de PDFs anexados compatíveis com separadores de caminho de Windows e Linux.

## Atualizar da versão anterior

Feche a versão antiga, extraia este pacote para uma nova pasta e execute `INICIAR.bat` no Windows. Os dados continuam na mesma pasta `PapelariaNfse` da conta Windows; não é necessário recadastrar ou importar registros. Execute somente uma versão por vez. As funções de arquivo, emissão manual e impressão foram mantidas. O formato de cadastros, atendimentos e referências de PDFs é compatível com as versões 0.1 e 0.2.

## Interface e navegação

- Menu lateral: Atendimento, Catálogo, Clientes, Histórico e Ajuda.
- Visual claro, azul e branco, fontes legíveis, ícones, campos com indicação de foco e tabelas mais organizadas.
- Atendimento com um botão Adicionar para código ou item selecionado, totais em destaque e descrição adicional dos serviços ao lado.
- Busca no catálogo, nos clientes e no histórico; busca por nome aceita texto sem acentos.
- Histórico com ações agrupadas no painel do atendimento selecionado; ações de PDF ficam disponíveis após anexar um arquivo.
- Edição de cadastro e detalhes do atendimento também por duplo clique.
- Atalhos: `F2` para o código, `Ctrl+S` para salvar na tela de atendimento, `Ctrl+1` a `Ctrl+5` para navegar.

`PREVIA.png` mostra a tela com itens demonstrativos. Os cadastros da imagem não são instalados no seu computador.

## Executar no Windows

1. Extraia o ZIP para uma pasta.
2. Tenha Java 17 ou mais recente instalado e disponível no comando `java`.
3. Clique duas vezes em `INICIAR.bat`.

Também é possível executar `java -jar papelaria.jar` em um terminal aberto nessa pasta. Não é um instalador EXE/MSI e o Java não está incluído. O JAR já está compilado; o código-fonte está na pasta `src`.

## Executar no Linux

Tenha Java 17 ou mais recente com suporte a interface gráfica (uma instalação somente headless não basta para abrir a janela) e uma sessão gráfica de desktop. Depois de extrair o ZIP, abra o terminal na pasta do aplicativo e execute:

```sh
sh INICIAR.sh
```

Ou execute diretamente:

```sh
java -jar papelaria.jar
```

Arquivos `.bat` são para Windows. A interface e os arquivos `.jar` são os mesmos nas duas plataformas. Para abrir navegador/PDF/pastas, tenha aplicativos padrão configurados; quando a API de desktop do Java não está disponível, o app tenta `xdg-open` no Linux.

A impressão física usa as impressoras instaladas no sistema. Configure a Elgin no sistema de impressão da sua distribuição e teste-a primeiro. Este pacote não instala driver de impressora; não foi feito teste físico da Elgin no Linux. A exportação de PDF de 58 × 210 mm funciona sem impressora instalada.

## Fluxo sem certificado digital

1. Em **Catálogo**, cadastre cada item com seu tipo correto. Exemplos: impressão A4 preto e branco (Serviço), impressão colorida (Serviço), caderno (Produto), cartolina (Produto), caneta (Produto).
2. Em **Clientes**, cadastre nome, CPF/CNPJ informado e contato, se desejar. O cadastro é opcional; o portal determina quais dados são exigidos em cada operação. O app não valida CPF/CNPJ nem consulta cadastros fiscais.
3. Em **Atendimento**, selecione o cliente, escolha os itens e indique quantidades. O preço fica registrado no atendimento. Leia um código de barras ou digite-o no campo Código. Um leitor configurado como teclado USB com sufixo Enter adiciona o item cadastrado. Códigos sem cadastro não consultam catálogos externos.
4. As observações são exclusivamente do serviço. Elas entram no texto preparado para a NFS-e; não use esse campo para inserir vendas de mercadorias.
5. Salve o atendimento. Em **Histórico**, selecione o atendimento, abra **Detalhes e descrição** e copie o texto dos serviços. Produtos não entram nessa descrição; os subtotais são separados na tela de atendimento e os valores de cada item estão nos detalhes.
6. Clique em **Abrir portal nacional**, acesse sua conta no navegador, preencha os dados fiscais, revise a descrição e o valor dos serviços, e emita a nota no portal. O app não preenche, autentica ou clica no portal automaticamente. Não armazena senha gov.br.
7. Baixe o PDF oficial e use **Anexar PDF oficial** para vinculá-lo ao atendimento. Informe a referência da nota se desejar. Verifique no PDF se o emitente, cliente, serviços e valores correspondem ao atendimento.
8. Abra o PDF, salve uma cópia ou reimprima. O botão **Imprimir PDF A4** abre o arquivo no leitor padrão do sistema; a impressão é concluída pelo usuário com Ctrl+P nesse leitor.

O estado exibido é **PDF anexado**, não “nota autorizada”. O app não verifica autenticidade fiscal, autorização, cancelamento ou correspondência de um PDF importado. A checagem do cabeçalho PDF apenas reconhece o formato do arquivo. O PDF oficial continua sendo aquele obtido do portal. Não há geração automática de NFS-e nesta versão.

## Elgin i7 e comprovante em bobina de 58 × 210 mm

Instale o driver Windows correspondente à variante exata da sua Elgin. Teste a impressão pelo Windows primeiro. No app, selecione o atendimento no **Histórico**, use **Comprovante térmico**, selecione a Elgin no diálogo de impressão e confira as preferências de papel de 58 × 210 mm do driver.

A rotina usa papel de 58 × 210 mm com área inicial de 52 mm; o driver determina a área efetivamente imprimível. Descrições compridas são quebradas e recibos compridos são paginados. A opção permite reimpressão do mesmo atendimento sem emitir outra nota. Não há comando de corte ESC/POS: a configuração do driver controla esse comportamento. A impressão real na Elgin ainda precisa ser verificada no seu Windows.

## Salvar o comprovante não fiscal em PDF

1. Salve o atendimento e abra **Histórico**.
2. Selecione o atendimento.
3. Clique em **Salvar PDF 58 × 210** e escolha a pasta e o nome do arquivo.
4. Quando quiser imprimir, abra o PDF no leitor de sua preferência, selecione a impressora térmica e o papel de 58 × 210 mm. Use **Tamanho real / 100%**, evitando “ajustar à página”.

O PDF tem tamanho fixo de 58 × 210 mm, em retrato, com margens de 5 mm e texto reforçado. O espaço restante fica em branco quando há poucos itens. Comprovantes longos são divididos em páginas do mesmo tamanho. Texto é selecionável, valores são preservados e acentos portugueses são suportados. Não requer internet, certificado digital ou impressora virtual. O leitor de PDF e o driver precisam aceitar o tamanho de papel correspondente; as configurações exatas dependem do modelo instalado.

O comprovante inclui serviços e produtos, total, data e cliente e, quando informada, a referência da NFS-e. Ele é identificado como **DOCUMENTO NÃO FISCAL** e não substitui a NFS-e. Gerá-lo não altera nem substitui o PDF oficial anexado ao atendimento.

`EXEMPLO-Comprovante.pdf` é uma amostra com dados demonstrativos; não é um documento de uma venda real.

A Nota Técnica SE/CGNFS-e nº 008, versão 1.0 de maio de 2026, determina no item 2.2.1 impressão do DANFSe em orientação retrato e papel de tamanho mínimo A4. Portanto, a bobina de aproximadamente 58 × 210 mm não serve para imprimir o DANFSe conforme esse leiaute. Preserve o PDF oficial e, para sua impressão oficial, use uma impressora A4.

O leitor de código de barras é um equipamento separado da impressora. Configure-o como teclado USB com sufixo Enter.

## Serviços e mercadorias

A NFS-e documenta os serviços sujeitos ao ISS, não as vendas separadas de cadernos, canetas e cartolinas. Para essas vendas, o app nesta versão oferece cadastro e registro comercial, sem emissão fiscal estadual. Mesmo num atendimento misto, a venda de mercadorias não deve ser acrescentada à NFS-e como serviço.

O enquadramento fiscal específico do serviço de impressão precisa corresponder à sua atividade e operação. Em São Paulo, a destinação de impressos personalizados, inclusive eventual comercialização/industrialização posterior, pode mudar o tratamento tributário. O app não escolhe automaticamente código de serviço, CNAE, NCM ou tributação; esses dados são revisados na emissão oficial.

## Dados e backup

No Windows, os dados ficam em `%LOCALAPPDATA%\PapelariaNfse`. Se essa variável não existir, ficam na pasta do usuário, em `PapelariaNfse`. No Linux, normalmente ficam em `~/PapelariaNfse`. O botão **Abrir pasta dos dados** mostra a pasta efetivamente usada.

Para transferir de Windows para Linux, feche o app nas duas máquinas e copie toda a pasta `PapelariaNfse`, incluindo `documentos`, para a pasta do usuário no Linux. Não há sincronização automática entre computadores. Esta versão reconhece as referências internas de PDF gravadas com separadores de Windows.

Os arquivos são locais. Cadastros e atendimentos usam arquivos UTF-8 e o PDF anexado é copiado para a pasta `documentos`; depois de anexar, mover o original de Downloads não quebra o vínculo. Uma atualização mantém a versão anterior do registro em `.bak`. PDFs anexados anteriormente são preservados quando outra versão é anexada.

Para backup, feche o app e copie **toda** a pasta `PapelariaNfse`, incluindo `documentos`, para um local seguro. Para restaurar, feche o app e recupere essa pasta a partir da cópia. Os arquivos não são criptografados; proteja sua conta Windows e suas cópias, sobretudo quando houver dados pessoais de clientes. Esta versão destina-se a um computador e uma instância do app por vez. Não use a mesma pasta simultaneamente em vários computadores.

O protótipo não gerencia estoque, pagamento integrado, exclusão de registros, rede multiusuário ou importação de XML fiscal. Não contém dados reais de clientes nem cadastros de exemplo. Arquivos malformados impedem a leitura e mostram erro, sem substituir os registros existentes.

## Limitações e evolução

O cadastro local pode ser mantido e a emissão manual substituída por integração oficial, mediante certificado digital adequado e implementação/testes da API. Conforme a Nota Técnica 2023.001, a emissão por API no Ambiente Nacional requer certificado, inclusive para MEI. Não existe integração de emissão implementada neste protótipo.

A etapa fiscal deverá incluir geração/validação da DPS, assinatura digital, comunicação autenticada, consulta antes de repetir um envio de resultado incerto, armazenamento de XML e chave de acesso, download do DANFSe, consulta de situação e tratamento de eventos. Testes devem ocorrer em ambiente de homologação antes de emitir documentos reais. O arquivo PDF sozinho não substitui esse fluxo.

## Desenvolvimento e validação

A interface usa Swing para que a base rode apenas com Java, sem baixar bibliotecas adicionais. Pode ser migrada para JavaFX posteriormente. A persistência inicial por arquivos evita dependências externas; SQLite e PDFBox podem ser incorporados ao ampliar o projeto.

Compile no Windows com `COMPILAR.bat` e execute os testes com `TESTAR.bat`. Para compilar, é necessário um JDK 17+ (os comandos `javac` e `jar`). Para rodar o JAR já pronto, basta Java 17+ com o módulo de interface gráfica. No Linux, os testes do JAR podem ser executados com `java -Djava.awt.headless=true -cp papelaria.jar br.com.papelaria.SelfTest` e `java -Djava.awt.headless=true -cp papelaria.jar br.com.papelaria.UiCheck`.

Os testes automatizados cobrem valores decimais, quantidades, cadastros com acentos, conservação do histórico após alterar o cadastro, separação entre serviços e produtos, preservação dos bytes do PDF, reconhecimento do cabeçalho PDF, backup de registro e largura/paginação da impressão térmica. Também verificam navegação, busca sem acentos, edição/seleção de registros filtrados, preservação da seleção, estado das ações de documentos, salvamento após uma busca e distribuição das telas em três tamanhos de janela.

Compilação, testes e renderização das telas foram executados em Linux com Java 17. Foram inspecionadas as telas em 1320×850, 1120×740 e 1080×720. Não foram realizados testes desta atualização no seu Windows, no portal autenticado ou em impressora física. A emissão automática não foi testada porque não está implementada. Os dados de teste são temporários; a prévia e o PDF de exemplo demonstram itens fictícios.

O PDF de comprovante foi verificado com dois leitores de estrutura/texto independentes e renderização visual, usando exemplos curto e longo. A verificação confirmou tamanho de 58 × 210 mm, ausência de texto fora da página, paginação, acentos, valores totais e presença do último item em um comprovante de 90 itens. Navegador e leitor PDF da sua distribuição não foram acionados neste ambiente sem sessão gráfica.

O gerador usa fontes PDF padrão Courier e codificação WinAnsi, que preserva os acentos portugueses. Caracteres fora dessa codificação podem aparecer como `?` no PDF do comprovante.

## Compilar no Linux

Com um **JDK 17 ou superior** instalado, abra o terminal na pasta do projeto:

```sh
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -d build/classes src/br/com/papelaria/*.java
jar --create --file papelaria.jar --main-class br.com.papelaria.App -C build/classes .
```

Para executar os testes sem abrir a janela:

```sh
java -Djava.awt.headless=true -cp papelaria.jar br.com.papelaria.SelfTest
java -Djava.awt.headless=true -cp papelaria.jar br.com.papelaria.UiCheck
```

## Problemas comuns

| Situação | Como resolver |
| --- | --- |
| Java não encontrado | Instale Java 17 ou superior e verifique com `java -version` |
| `UnsupportedClassVersionError` | A versão do Java é antiga; use Java 17 ou superior |
| A janela não abre no Linux | Use uma sessão gráfica e uma instalação Java com suporte gráfico |
| PDF ou navegador não abre | Configure aplicativos padrão; no Linux, confira se `xdg-open` está disponível |
| Código não cadastrado | Cadastre o código no item antes de utilizá-lo no atendimento |
| Ações do PDF estão desabilitadas | Selecione o atendimento e anexe o PDF oficial primeiro |
| Comprovante muito pequeno | No leitor PDF, escolha papel de 58 × 210 mm e tamanho real / 100% |
| Impressora não aparece | Instale e teste a impressora no sistema operacional |
| Dados não aparecem após trocar de computador | Copie toda a pasta `PapelariaNfse`, incluindo `documentos`, para a pasta correta |

## Referências oficiais

Documentação consultada em 11/10/2026.


- [Emissor Nacional](https://www.nfse.gov.br/EmissorNacional)
- API atual: https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/documentacao-atual
- Certificado para API do MEI, NT 2023.001: https://www.gov.br/nfse/pt-br/nt-2023-001-obrigatoriedade-da-nfs-e-nacional-para-o-mei.pdf
- Leiaute DANFSe / papel mínimo A4, NT 008: https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/rtc/nt-008-se-cgnfse-danfse-20260505.pdf
- Separação NFS-e e mercadorias: https://www8.receita.fazenda.gov.br/simplesnacional/noticias/NoticiaCompleta.aspx?id=bd3077db-2db0-47af-a1f6-2b9d32f9514a
- Exemplo oficial de interpretação sobre impressos personalizados em SP: https://legislacao.fazenda.sp.gov.br/Paginas/RC31908_2025.aspx
- Suporte e drivers Elgin: https://www.elgin.com.br/download-center

Antes de colocar a emissão fiscal automática em produção, confira as versões e regras em vigor.
