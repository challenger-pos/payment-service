resource "kubernetes_service" "billing" {
  metadata {
    name      = "${var.service_name}-service"
    namespace = kubernetes_namespace.billing.metadata[0].name
    
    labels = {
      app     = var.service_name
      service = var.service_name
    }

    annotations = {
      "service.beta.kubernetes.io/aws-load-balancer-type"                              = "nlb"
      "service.beta.kubernetes.io/aws-load-balancer-nlb-target-type"                   = "instance"
      "service.beta.kubernetes.io/aws-load-balancer-scheme"                            = "internal"
      "service.beta.kubernetes.io/aws-load-balancer-backend-protocol"                  = "http"
      "service.beta.kubernetes.io/aws-load-balancer-healthcheck-path"                  = "/actuator/health"
      "service.beta.kubernetes.io/aws-load-balancer-healthcheck-interval"              = "30"
      "service.beta.kubernetes.io/aws-load-balancer-healthcheck-timeout"               = "10"
      "service.beta.kubernetes.io/aws-load-balancer-healthcheck-healthy-threshold"     = "2"
      "service.beta.kubernetes.io/aws-load-balancer-healthcheck-unhealthy-threshold"   = "2"
      "service.beta.kubernetes.io/aws-load-balancer-manage-backend-security-group-rules" = "true"
    }
  }

  spec {
    selector = {
      app = var.service_name
    }

    port {
      name        = "http"
      protocol    = "TCP"
      port        = 80
      target_port = var.app_port
    }

    type                    = "LoadBalancer"
    session_affinity        = "None"
    external_traffic_policy = "Cluster"
  }

  depends_on = [
    kubernetes_deployment.billing
  ]
}
