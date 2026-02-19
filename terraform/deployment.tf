resource "kubernetes_deployment" "billing" {
  metadata {
    name      = var.service_name
    namespace = kubernetes_namespace.billing.metadata[0].name
    
    labels = {
      app         = var.service_name
      service     = var.service_name
      environment = var.environment
      version     = "v1"
    }
  }

  wait_for_rollout = false

  spec {
    replicas = var.app_replicas

    selector {
      match_labels = {
        app = var.service_name
      }
    }

    template {
      metadata {
        labels = {
          app         = var.service_name
          service     = var.service_name
          environment = var.environment
          version     = "v1"

          # Datadog labels
          "tags.datadoghq.com/env"     = var.environment
          "tags.datadoghq.com/service" = var.service_name
          "tags.datadoghq.com/version" = var.app_version
          "admission.datadoghq.com/enabled" = "true"
        }

        annotations = {
          "prometheus.io/scrape" = "true"
          "prometheus.io/port"   = tostring(var.app_port)
          "prometheus.io/path"   = "/actuator/prometheus"
        }
      }

      spec {
        # Volume for Datadog Java Agent
        volume {
          name = "dd-java-agent"
          empty_dir {}
        }

        # InitContainer: download Datadog Java Agent
        init_container {
          name  = "dd-java-agent-init"
          image = "curlimages/curl:8.10.1"
          command = ["sh", "-c", "curl -L -o /dd/dd-java-agent.jar https://dtdg.co/latest-java-tracer"]
          volume_mount {
            name       = "dd-java-agent"
            mount_path = "/dd"
          }
        }

        container {
          name              = var.service_name
          image             = var.app_image
          image_pull_policy = "Always"

          port {
            container_port = var.app_port
            protocol       = "TCP"
            name           = "http"
          }

          # Datadog Java Agent and env vars
          env {
            name  = "JAVA_TOOL_OPTIONS"
            value = "-javaagent:/dd/dd-java-agent.jar"
          }
          env {
            name  = "DD_SERVICE"
            value = var.service_name
          }
          env {
            name  = "DD_ENV"
            value = var.environment
          }
          env {
            name  = "DD_VERSION"
            value = var.app_version
          }
          env {
            name  = "DD_LOGS_INJECTION"
            value = "true"
          }
          env {
            name  = "DD_AGENT_HOST"
            value = var.datadog_agent_host
          }
          env {
            name  = "DD_DOGSTATSD_PORT"
            value = "8125"
          }
          env {
            name  = "DATADOG_STATSD_HOST"
            value = var.datadog_agent_host
          }
          env {
            name  = "DATADOG_STATSD_PORT"
            value = "8125"
          }

          # Environment Variables from ConfigMap
          env_from {
            config_map_ref {
              name = kubernetes_config_map.billing.metadata[0].name
            }
          }

          # Environment Variables from Secret
          env_from {
            secret_ref {
              name = kubernetes_secret.billing.metadata[0].name
            }
          }

          volume_mount {
            name       = "dd-java-agent"
            mount_path = "/dd"
          }

          # Resources
          resources {
            requests = {
              cpu    = "250m"
              memory = "512Mi"
            }
            limits = {
              cpu    = "1000m"
              memory = "1Gi"
            }
          }

          # Startup Probe (Spring Boot demora para iniciar)
          startup_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = var.app_port
            }
            initial_delay_seconds = 60
            period_seconds        = 10
            timeout_seconds       = 5
            failure_threshold     = 30
          }

          # Liveness Probe
          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = var.app_port
            }
            initial_delay_seconds = 30
            period_seconds        = 30
            timeout_seconds       = 5
            failure_threshold     = 3
          }

          # Readiness Probe
          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = var.app_port
            }
            initial_delay_seconds = 30
            period_seconds        = 10
            timeout_seconds       = 5
            failure_threshold     = 3
          }
        }

        # Pod Security
        security_context {
          run_as_non_root = true
          run_as_user     = 1000
          fs_group        = 1000
        }
      }
    }

    strategy {
      type = "RollingUpdate"
      
      rolling_update {
        max_surge       = "1"
        max_unavailable = "0"
      }
    }
  }

  depends_on = [
    kubernetes_config_map.billing,
    kubernetes_secret.billing
  ]
}
