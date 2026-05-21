{{- /*
MSP Helm Templates
*/ -}}
{{- define "msp.labels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/part-of: {{ .Release.Name }}
heritage: {{ .Release.Service }}
{{- end }}

{{- /*
MySQL Deployment
*/ -}}
{{- define "msp.mysql.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "msp.fullname" . }}-mysql
  labels:
    {{- include "msp.labels" . | nindent 4 }}
    component: mysql
spec:
  replicas: {{ .Values.mysql.replicaCount }}
  selector:
    matchLabels:
      {{- include "msp.selectorLabels" . | nindent 6 }}
      component: mysql
  template:
    metadata:
      labels:
        {{- include "msp.selectorLabels" . | nindent 8 }}
        component: mysql
    spec:
      containers:
        - name: mysql
          image: "{{ .Values.mysql.image.repository }}:{{ .Values.mysql.image.tag }}"
          imagePullPolicy: IfNotPresent
          args:
            - "--default-authentication-plugin=mysql_native_password"
            - "--character-set-server=utf8mb4"
            - "--collation-server=utf8mb4_unicode_ci"
          ports:
            - containerPort: 3306
              name: mysql
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: "root123456"
            - name: MYSQL_DATABASE
              value: {{ .Values.mysql.database }}
            - name: MYSQL_USER
              value: {{ .Values.mysql.username }}
            - name: MYSQL_PASSWORD
              value: {{ .Values.mysql.password }}
          volumeMounts:
            - name: mysql-data
              mountPath: /var/lib/mysql
          resources:
            {{- toYaml .Values.mysql.resources | nindent 12 }}
      volumes:
        - name: mysql-data
          {{- if .Values.mysql.persistence.enabled }}
          persistentVolumeClaim:
            claimName: {{ include "msp.fullname" . }}-mysql
          {{- else }}
          emptyDir: {}
          {{- end }}
---
apiVersion: v1
kind: Service
metadata:
  name: {{ include "msp.fullname" . }}-mysql
  labels:
    {{- include "msp.labels" . | nindent 4 }}
    component: mysql
spec:
  type: {{ .Values.mysql.service.type }}
  ports:
    - port: {{ .Values.mysql.service.port }}
      targetPort: 3306
      protocol: TCP
      name: mysql
  selector:
    {{- include "msp.selectorLabels" . | nindent 4 }}
    component: mysql
{{- if .Values.mysql.persistence.enabled }}
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: {{ include "msp.fullname" . }}-mysql
  labels:
    {{- include "msp.labels" . | nindent 4 }}
    component: mysql
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: {{ .Values.mysql.persistence.size }}
  {{- if .Values.mysql.persistence.storageClass }}
  storageClassName: {{ .Values.mysql.persistence.storageClass }}
  {{- end }}
{{- end }}
{{- end }}