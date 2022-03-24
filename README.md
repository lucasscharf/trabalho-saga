# Trabalho de SAGA

Esse projeto contém as dependências para a realização de um sistema simples de cadastro e liberação de usuário para utilização de um único curso. Abaixo vemos a descrição de cada um dos serviços e como eles funcionam. Para fazer a interligação dos módulos é utilizado um servidor Apache Kafka. As interações com esses módulos podem ser feitos via endpoint REST.
Cada serviço que possui interação com REST possuei uma interface swagger acessível via localhost:<porta do serviço>/swagger-ui.

## Serviço de inscrição
Porta 8080
```
recebe os dados necessários para inscrição no curso e cria um registro de inscrição no sistema, com estado 'pendente'; somente ao final da transação esse estado será modificado para 'vigente'; em caso de falha nos passos subsequentes, o estado será modificado para 'cancelada'. 
```

O serviço de inscrição possui 3 endpoints: 
* POST /inscricao: cria uma nova inscrição com status Pendente e dispara uma mensagem na fila de inscricao realizada
* GET /inscricao: retorna a lista de todas as inscrições realizadas
* GET /{id}: retorna a inscrição com um determinado id

Além disso, esse módulo ouve o tópico de inscrição atualizada e se atualiza caso seja feito alguma mudança na inscrição. 


## Serviço de pagamento 
Porta 8085
```
responsável por processar o pagamento da taxa de inscrição no curso; deve cancelar a inscrição caso o pagamento (por meio de cartão de crédito) seja recusado. 
```

O serviço de pagamento possui 4 endpoints:

* POST /pagamento/pagarComSucesso/{id}
* POST /pagamento/pagarComFalha/{id}
* GET  /pagamento/inscricao
* GET  /pagamento
* GET  /pagamento/{idPagamento}

O endpoint de pagar com falha recebem como objeto uma String com a descrição de porque o pagamento não deu certo (dígito verificador errado, saldo insuficiente, etc...). Após um pagamento ser realizado com sucesso, é disparado uma mensagem no tópico pagamento-realizado e outro em inscricao-atualizada. Caso um pagamento seja realizado com falha, então não é enviado nenhuma mensagem no tópico de pagamento-realizado, porém é enviado uma mensagem no tópico inscricao-atualizada. O id referenciado nesses casos é o id da inscrição. Lembrando que se houver mais de duas inscrições com o mesmo ID, apenas a inscrição mais recente é que é considerada.

Além disso, a criação de um pagamento com sucesso, cria uma entidade pagamento no banco de dados. É possível consultar um determinado pagamento específico (/pagamento/{idPagamento}) ou a lista de todos os pagamentos (/pagamento). Também é possível verificar as inscrições que o sistema tem o registro no momento (/pagamento/inscricao). O sistema não impede que sejam feitos pagamentos de inscrições já pagas ou canceladas. Caso queira-se fazer algo assim, é necessário utilizar o endpoint de pagamento com falha. O sistema salva todos os pagamentos realizados ou cancelados. Caso exista alguma alteração em um determinado pagamento (vindo via sistema de mensageria), o sistema usará o número do ID do pagamento para identificar o pagamento e fazer as alterações.

O sistema não impede que sejam feitos dois pagamentos seguidos da mesma inscrição ou um pagamento e um cancelamento. Nesse tipo de situação a atualização mais recente da inscrição/pagamento é o que estará valendo.

Além disso, o serviço fica ouvindo o tópico de atualização de pagamento para verificar o status de seus pagamentos.

## Serviço de emissão de notas
Porta 8090

``` 
gera uma nota fiscal de serviço para o curso adquirido pelo usuário; em caso de falha na geração da nota fiscal (por exemplo, caso o usuário tenha fornecido um CPF incorreto), o pagamento e a inscrição devem ser cancelados. 
```

O serviço de emissão funciona aos mesmos moldes do serviço de pagamento no que tange e fazer um método de emissão com sucesso e outro com falha.
Ele mantém apenas o registro do último registro de pagamento, incluindo o status de emitido ou não. Caso seja gerado um novo pagamento com o mesmo id de pagamento, então o registro local será sobreescrito.

* POST /emissao/emitirComSucesso/{idPagamento}
* POST /emissao/emitirComFalha/{idPagamento}
* GET  /emissao

Quando um pagamento é realizado com falha, ele atualiza o registro do pagamento no seu banco de dados bem como manda uma mensagem aos tópicos pagamento-atualizado e inscricao-atualizado.
Quando um pagamento é realizado com sucesso, ele atualiza o registro do pagamento no seu banco de dados bem como manda mensagens aos tópicos: pagamento-atualizado, inscricao-atualizada e emissao-realizada. 

Caso, por qualquer motivo, o sistema receba dois pagamentos para o mesmo id de pagamento o sistema irá considerar apenas o registro mais recente.
## Serviço de acesso

Porta 9000

```
libera o acesso ao conteúdo do curso para o usuário, concluindo a transação.
```

Esse serviço tem apenas um único endpoint.

* GET /acesso/{id}

Onde o id é o id da inscrição. Esse endpoint retorna true se o usuário pode acessar o sistema ou false caso contrário. Caso uma inscrição receba um pagamento com sucesso e um pagamento com falha, será considerado apenas o pagamento com sucesso. Independemtente da ordem das operações. 

## Serviço de logs
Porta 9100.

Esse serviço vem cumprir o requisito de saída de dados. 

```
O processamento da transação em cada subsistema deve ser registrado por meio de mensagens impressas na tela, em uma interface gráfica simples ou em arquivos de log;
```

Um serviço simples que ouve todos os tópicos e informa a mensagem que atravessou em cada tópico. Ele ouve todos os tópicos da aplicação e informa na saída padrão qual é a mensagem e qual é o tópico. Com isso, não será necessário olhar os logs individuais de cada aplicação.

# Decisões de Design
Os payloads das mensagens são os mesmos em diferentes tópicos (inscrição/pagamento) é utilizada para a comunicação. 
Num ambiente real, cada tópico teria um payload específico focado para as suas necessidades.

A mesma entidade salva no banco de dados é utilizada na interface REST. 
Os dados são sanitizados removendo qualquer informação irrelevante da entidade, antes de fazer a persistência. 
Existe muita discussão sobre a validade dessa abordagem, principalmente num ambiente de microsserviços. 

O banco de dados é apenas um Set em memória. Ele não provê nenhum tipo de persistência e pode existir algum tipo de inconsistência entre os dados. Principalmente num ambiente com várias instâncias diferentes do mesmo serviço rodando. 

O paralelismo das funcionalidades vem da utilização de tópicos Kafka bem configurados. Principalmente no que tange o número de partições Kafka. Caso o tópico tenha apenas uma única partição, não haverá paralelismo, mesmo que adicionemos mais instâncias rodando. 

# Requisitos do projeto

```
A interação entre os subsistemas deve ser feita utilizando uma das tecnologias vistas ao longo da disciplina (inclusive nos seminários);
Pode ser usada qualquer linguagem de programação de alto nível (ou seja, legível por humanos);
```

As tecnologias usadas são Java (usando o framework Quarkus) e Kafka (um sistema de event streaming).

```
Cada subsistema deve ser implementado em um processo independente, podendo ser executados na mesma máquina ou em máquinas virtuais/contêineres/etc. diferentes (opte pelo que lhe parecer mais prático);
```
Cada serviço roda em um processo separado. É usando um único script de inicialização para facilitar a utilização. É possível colocar esses seriviços em containers, porém optou-se por rodar diretamente com o comando ``mvn quarkus:dev``.

```
O sistema (e cada um de seus subsistemas) deve suportar a execução de transações de forma concorrente;
```

O serviço utiliza Kafka, colocando-se a aplicação com diferentes partições, teremos o paralelismo.

```
O processamento da transação em cada subsistema deve ser registrado por meio de mensagens impressas na tela, em uma interface gráfica simples ou em arquivos de log;
```

O serviço de logs cumpre esse objetivo.

```
A falha na execução das operações pode ser determinada de forma aleatória ou por meio da interface do programa;
```
Todas as operações falhas geram eventos com status de falhas para que os sistemas façam as operações de compensação.

```
Crie um programa de teste que envie requisições ao sistema a uma taxa que permita acompanhar o andamento da execução.
```

Foram criados scripts para fazer as operações na linha de comando. Também é possível executar elas pela interface de swagger.

# Tópicos

|Nome do Tópico|Descrição|
|-----  |----- |
|inscricao-realizada | Utilizido ao fazer uma nova inscrição|
|pagamento-realizado | Utilizado caso ocorra tudo certo com o pagamento|
|emissao-realizada | Utilizado caso ocorra tudo certo com a emissão da nota|
|inscricao-atualizada | Utilizado para informar que houve alguma atualização na inscrição|
|pagamento-atualizado | Utilizado para informar que houve alguma atualização no pagamento|
