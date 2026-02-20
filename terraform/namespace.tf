resource "kubernetes_namespace" "billing" {
  metadata {
    name = "${var.project_name}-${var.service_name}"
    
    labels = {
      name        = var.service_name
      environment = var.environment
      managed-by  = "terraform"
    }
  }
}
