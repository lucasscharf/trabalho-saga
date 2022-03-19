# 

## Serviço de inscrição

## Serviço de emissão de notas

## Serviço de pagamento 

## Serviço de acesso

## Serviço de logs
Um serviço simples que ouve todos os tópicos e informa a mensagem que atravessou em cada log.

# Tópicos
Por decisão de design e simplicidade, a mesma entidade (inscrição) é utilizada para a comunicação. 
Num ambiente real, cada tópico teria um payload específico.

|---|---|
|Nome do Tópico|Descrição|
|inscricao-realizada|Utilizida ao fazer uma nova inscrição|
|inscricao-cancelada|Utilizada caso ocorra algum problema na inscrição e essa precise ser cancelada|
|pagamento-realizado|Utilizada caso ocorra tudo certo com o pagamento|



