{{/*
Retrieves the key value from a K8s Secret
The namespace of the Secret is provided by the NS parameter
The name of the Secret is provided by the Secret parameter
The secret key is provided by the Key parameter
*/}}
{{- define "getIspnSecret" }}
{{- $obj := (lookup "v1" "Secret" .NS .Secret).data -}}
{{- if $obj }}
{{- index $obj .Key | b64dec -}}
{{- else -}}
{{- "<not found>" -}}
{{- end -}}
{{- end }}