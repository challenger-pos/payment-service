resource "kubernetes_horizontal_pod_autoscaler_v2" "billing" {
  metadata {
    name      = "${var.service_name}-hpa"
    namespace = kubernetes_namespace.billing.metadata[0].name
  }

  spec {
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment.billing.metadata[0].name
    }

    min_replicas = var.hpa_min_replicas
    max_replicas = var.hpa_max_replicas

    metric {
      type = "Resource"
      
      resource {
        name = "cpu"
        
        target {
          type                = "Utilization"
          average_utilization = var.hpa_cpu_threshold
        }
      }
    }

    metric {
      type = "Resource"
      
      resource {
        name = "memory"
        
        target {
          type                = "Utilization"
          average_utilization = 80
        }
      }
    }

    behavior {
      scale_down {
        stabilization_window_seconds = 300
        
        policy {
          type           = "Percent"
          value          = 50
          period_seconds = 60
        }
        select_policy = "Max"
      }

      scale_up {
        stabilization_window_seconds = 0
        
        policy {
          type           = "Percent"
          value          = 100
          period_seconds = 60
        }
        
        policy {
          type           = "Pods"
          value          = 2
          period_seconds = 60
        }
        
        select_policy = "Max"
      }
    }
  }

  depends_on = [
    kubernetes_deployment.billing
  ]
}
