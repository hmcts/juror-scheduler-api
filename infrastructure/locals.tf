locals {
  vault_name = "${var.product}-${var.env}"
  rg_name    = "${var.product}-${var.env}-rg"
  db_name    = "scheduler_application"
  db_port    = 5432
}