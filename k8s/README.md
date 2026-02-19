# Kubernetes manifests (payment-service)

Estrutura alinhada ao challengeOne e stock-service: namespace, ConfigMap, Secret, Deployment (com Datadog), Service e HPA.

## Ordem de aplicação

1. **Namespace:** `kubectl apply -f payment-service-namespace.yaml`
2. **Datadog Agent** (uma vez por cluster, namespace default):
   `kubectl create secret generic datadog-secret -n default --from-literal=api-key=SEU_DD_API_KEY`
   `kubectl apply -f datadog-agent.yaml`
3. **Secret:** edite `payment-service-secret.yaml` (substitua REPLACE_*), ou use:
   `kubectl create secret generic payment-service-secret -n payment --from-literal=AWS_ACCESS_KEY=xxx --from-literal=AWS_SECRET_KEY=xxx --from-literal=MERCADOPAGO_ACCESS_TOKEN=xxx --from-literal=MERCADOPAGO_PUBLIC_KEY=xxx`
4. **ConfigMap:** `kubectl apply -f payment-service-configmap.yaml`
5. **Deployment:** substitua `REPLACE_IMAGE` pela imagem do payment-service (ex.: `seu-registry/payment-service:1.0.0`), depois:
   `kubectl apply -f payment-service-deployment.yaml`
6. **Service:** `kubectl apply -f payment-service-service.yaml`
7. **HPA:** `kubectl apply -f payment-service-hpa.yaml`

Ou aplique a pasta (após editar Secret e imagem):
`kubectl apply -f k8s/`

## Ajustes

- **ConfigMap:** altere nomes das filas SQS, tabela DynamoDB e região AWS se necessário.
- **Secret:** não versionar valores reais; use `kubectl create secret` ou um fluxo seguro.
- **Deployment:** `image` deve apontar para a imagem do payment-service (ex.: `seu-registry/payment-service:1.0.0`).
- **NodePort:** o Service usa `30082`; altere se houver conflito com outros serviços (stock-service usa 30081).
