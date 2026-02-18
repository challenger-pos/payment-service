output "namespace" {
  description = "Kubernetes namespace"
  value       = kubernetes_namespace.billing.metadata[0].name
}

output "service_name" {
  description = "Kubernetes service name"
  value       = kubernetes_service.billing.metadata[0].name
}

output "service_cluster_ip" {
  description = "Service ClusterIP"
  value       = kubernetes_service.billing.spec[0].cluster_ip
}

output "loadbalancer_hostname" {
  description = "Load Balancer hostname (DNS)"
  value       = try(kubernetes_service.billing.status[0].load_balancer[0].ingress[0].hostname, "Aguardando DNS...")
}

output "deployment_name" {
  description = "Deployment name"
  value       = kubernetes_deployment.billing.metadata[0].name
}
