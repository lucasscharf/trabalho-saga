# Trabalho de SAGA

Esse projeto contém as dependências para a realização de um sistema simples de cadastro e liberação de usuário para utilização de um único curso. Abaixo vemos a descrição de cada um dos serviços e como eles funcionam. Para fazer a interligação dos módulos é utilizado um servidor Apache Kafka. As interações com esses módulos podem ser feitos via endpoint REST.
Cada serviço que possui interação com REST possuei uma interface swagger acessível via localhost:<porta do serviço>/swagger-ui.

## Serviço de inscrição
Porta 8080
```
recebe os dados necessários para inscrição no curso e cria um registro de inscrição no sistema, com estado 'pendente'; somente ao final da transação esse estado será modificado para 'vigente'; em caso de falha nos passos subsequentes, o estado será modificado para 'cancelada'. 
```

O serviço de inscrição possui 3 endpoints: 
POST /inscricao: cria uma nova inscrição com status Pendente e dispara uma mensagem na fila de inscrição realizada
GET /inscrição: retorna a lista de todas as inscrições realizadas
GET /{id}: retorna a inscrição com um determinado id

Além disso, esse módulo ouve o tópico de inscrição atualizada e se atualiza caso seja feito alguma mudança na inscrição. 


## Serviço de pagamento 
Porta 8085
```
responsável por processar o pagamento da taxa de inscrição no curso; deve cancelar a inscrição caso o pagamento (por meio de cartão de crédito) seja recusado. 
```

O serviço de pagamento possui 4 endpoints:

POST /pagamento/pagarComSucesso/{id}
POST /pagamento/pagarComFalha/{id}
GET  /pagamento/inscricao
GET  /pagamento
GET  /pagamento/{idPagamento}

Os endpoints de pagar com falha recebem como objeto uma String com a descrição de porque o pagamento não deu certo (dígito verificador errado, saldo insuficiente, etc...). Após um pagamento ser realizado com sucesso, é disparado uma mensagem no tópico pagamento-realizado e outro em inscricao-atualizada. Caso um pagamento seja realizado com falha, então não é enviado nenhuma mensagem no tópico de pagamento-realizado, porém é enviado uma mensagem no tópico inscricao-atualizada.

Além disso, a criação de um pagamento com sucesso, cria uma entidade pagamento no banco de dados. É possível consultar um determinado pagamento específico (/pagamento/{idPagamento}) ou a lista de todos os pagamentos (/pagamento). Também é possível verificar as inscrições que o sistema tem o registro no momento (/pagamento/inscricao). O sistema não impede que sejam feitos pagamentos de inscrições já pagas ou canceladas. Caso queira-se fazer algo assim, é necessário utilizar o endpoint de pagamento com falha. O sistema salva todos os pagamentos realizados ou cancelados. Caso exista alguma alteração em um determinado pagamento (vindo via sistema de mensageria), o sistema irá alterar o pagamento mais recente.

O sistema não impede que sejam feitos dois pagamentos seguidos da mesma inscrição ou um pagamento e um cancelamento. Nesse tipo de situação a atualização mais recente da inscrição/pagamento é o que estará valendo.

Além disso, o serviço fica ouvindo o tópico de atualização de pagamento para verificar o status de seus pagamentos.

## Serviço de emissão de notas
Porta 8090

``` gera uma nota fiscal de serviço para o curso adquirido pelo usuário; em caso de falha na geração da nota fiscal (por exemplo, caso o usuário tenha fornecido um CPF incorreto), o pagamento e a inscrição devem ser cancelados. ```

O serviço de emissão funciona aos mesmos moldes do serviço de pagamento. O que muda é o nome dos endpoints.

POST /emissao/emitirComSucesso/{id}
POST /emissao/emitirComFalha/{id}
GET  /emissao
GET  /emissao/{id}

O nome do tópico de saída também é diferente.
## Serviço de acesso



## Serviço de logs
Um serviço simples que ouve todos os tópicos e informa a mensagem que atravessou em cada tópico. Ele ouve todos os tópicos da aplicação e informa na saída padrão qual é a mensagem e qual é o tópico. Com isso, não será necessário 
# Decisões de Design
Os payloads das mensagens são os mesmos em diferentes tópicos (inscrição/pagamento) é utilizada para a comunicação. 
Num ambiente real, cada tópico teria um payload específico focado para as suas necessidades.

A mesma entidade salva no banco de dados é utilizada na interface REST. 
Os dados são sanitizados removendo qualquer informação irrelevante da entidade, antes de fazer a persistência. 
Existe muita discussão sobre a validade dessa abordagem, principalmente num ambiente de microsserviços. 

O banco de dados é apenas um Set em memória. Ele não provê nenhum tipo de persistência e pode existir algum tipo de inconsistência entre os dados. Principalmente num ambiente com várias instâncias diferentes do mesmo serviço rodando. 

O paralelismo das funcionalidades vem da utilização de tópicos Kafka bem configurados. Principalmente no que tange o número de partições Kafka. Caso o tópico tenha apenas uma única partição, não haverá paralelismo, mesmo que adicionemos mais instâncias rodando. 
## Tópicos

|Nome do Tópico|Descrição|
|-----  |----- |
|inscricao-realizada | Utilizido ao fazer uma nova inscrição|
|inscricao-cancelada | Utilizado caso ocorra algum problema na inscrição e essa precise ser cancelada|
|pagamento-realizado | Utilizado caso ocorra tudo certo com o pagamento|
|emissao-realizada | Utilizado caso ocorra tudo certo com a emissão da nota|
|inscricao-atualizada | Utilizado para informar que houve alguma atualização na inscrição|
|pagamento-atualizado | Utilizado para informar que houve alguma atualização no pagamento|
