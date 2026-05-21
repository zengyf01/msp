{{/*
Expand the name of this chart
*/}}
{{- define "msp.fullname" -}}
{{- default .Chart.Name .Values.app.name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "msp.selectorLabels" -}}
app.kubernetes.io/name: {{ include "msp.fullname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create a default fully qualified app name
*/}}
{{- define "msp.mysql.fullname" -}}
{{- printf "%s-mysql" (include "msp.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}