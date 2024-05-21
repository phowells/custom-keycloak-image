# custom-keycloak-image

# Minikube Setup on Macbook Air
https://devopscube.com/minikube-mac/

# Starting Minikube in MacOs
<p>
We need to blow away the minikube config before restarting due to firewall restrictions that cannot be updated.
</p>

>minikube stop<br/>
minikube delete<br/>
brew uninstall minikube<br/>
rm -rf ~/.minikube<br/>
sudo brew services stop socket_vmnet<br/>
brew uninstall socket_vmnet<br/>
sudo rm -rf /opt/homebrew/Cellar/socket_vmnet/1.1.4<br/>
brew uninstall qemu

Restart Computer...

>brew install qemu<br/>
brew install socket_vmnet<br/>
brew tap homebrew/services<br/>
HOMEBREW=$(which brew) && sudo ${HOMEBREW} services start socket_vmnet<br/>
brew install minikube<br/>
minikube start --driver qemu --network socket_vmnet<br/>
minikube status<br/>

# Configure Ingress with Self -Signed Cert
https://supportportal.juniper.net/s/article/Generate-a-self-signed-SSL-certificate-in-PEM-format-using-OpenSSL?language=en_US<br/>
Generate the Key in the 'helm' directory of the project
>openssl genrsa -aes256 -out encrypted-key.pem

Remove the password
>openssl rsa -in encrypted-key.pem -out key.pem

Generate the certificate
>openssl req -new -x509 -key key.pem -out cert.pem -days 1095

https://minikube.sigs.k8s.io/docs/tutorials/custom_cert_ingress/<br/>
Create TLS secret which contains custom certificate and private key
>kubectl -n kube-system create secret tls mkcert --key key.pem --cert cert.pem

Configure ingress addon
>minikube addons configure ingress<br/>
-- Enter custom cert (format is "namespace/secret"): kube-system/mkcert<br/>
✅  ingress was successfully configured<br/>

Enable ingress addon (disable first when already enabled)
>minikube addons disable ingress<br/>
>minikube addons enable ingress<br/>

Verify if custom certificate was enabled
>kubectl -n ingress-nginx get deployment ingress-nginx-controller -o yaml | grep "kube-system"<br/>
>--default-ssl-certificate=kube-system/mkcert<br/>

# Configure Minikube to use Red Hat Private Registry
>minikube addons configure registry-creds</br>

# Update custom-keycloak-ingress.yaml with minikube ip.
minikube ip</br>

# Deploy to Kubernetes
kubectl apply -f custom-keycloak.yaml<br/>
kubectl apply -f custom-keycloak-ingress.yaml<br/>

# Access Admin Console
KEYCLOAK_URL=https://custom-keycloak.$(minikube ip).nip.io &&
echo "" &&
echo "Keycloak:                 $KEYCLOAK_URL" &&
echo "Keycloak Admin Console:   $KEYCLOAK_URL/admin" &&
echo "Keycloak Account Console: $KEYCLOAK_URL/realms/myrealm/account" &&
echo ""

# Restart Keycloak
kubectl rollout restart deployment custom-keycloak<br/>

# Keycloak SAML Chaining Video
https://www.youtube.com/watch?v=JBAKnJ9Obvw
