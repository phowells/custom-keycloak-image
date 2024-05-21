package com.paulhowells.keycloak;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "EntityDescriptor")
public class SamlEntityDescriptor {

    @JacksonXmlProperty(localName = "entityID", isAttribute = true)
    public String entityId;

    @JacksonXmlProperty(localName = "ID", isAttribute = true)
    public String id;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "IDPSSODescriptor")
    public List<IdpssoDescriptor> idpssoDescriptors = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "SPSSODescriptor")
    public List<SPSSODescriptor> spssoDescriptors = new ArrayList<>();

    public static class IdpssoDescriptor {

        @JacksonXmlProperty(localName = "WantAuthnRequestsSigned", isAttribute = true)
        public String wantAuthnRequestsSigned;

        @JacksonXmlProperty(localName = "protocolSupportEnumeration", isAttribute = true)
        public String protocolSupportEnumeration;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "KeyDescriptor")
        public List<KeyDescriptor> keyDescriptors = new ArrayList<>();

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "ArtifactResolutionService")
        public List<ArtifactResolutionService> artifactResolutionServices = new ArrayList<>();

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "SingleLogoutService")
        public List<SingleLogoutService> singleLogoutServices = new ArrayList<>();

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "NameIDFormat")
        public List<String> nameIDFormats = new ArrayList<>();

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "SingleSignOnService")
        public List<SingleSignOnService> singleSignOnServices = new ArrayList<>();
    }

    public static class SPSSODescriptor {

        @JacksonXmlProperty(localName = "protocolSupportEnumeration", isAttribute = true)
        public String protocolSupportEnumeration;

        @JacksonXmlProperty(localName = "AuthnRequestsSigned", isAttribute = true)
        public String authnRequestsSigned;

        @JacksonXmlProperty(localName = "WantAssertionsSigned", isAttribute = true)
        public String wantAssertionsSigned;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "KeyDescriptor")
        public List<KeyDescriptor> keyDescriptors = new ArrayList<>();

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "SingleLogoutService")
        public List<SingleLogoutService> singleLogoutServices = new ArrayList<>();

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "NameIDFormat")
        public List<String> nameIDFormats = new ArrayList<>();

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "AssertionConsumerService")
        public List<AssertionConsumerService> assertionConsumerServices = new ArrayList<>();
    }

    public static class AssertionConsumerService {

        @JacksonXmlProperty(localName = "Binding", isAttribute = true)
        public String binding;

        @JacksonXmlProperty(localName = "Location", isAttribute = true)
        public String location;

        @JacksonXmlProperty(localName = "isDefault", isAttribute = true)
        public String isDefault;

        @JacksonXmlProperty(localName = "index", isAttribute = true)
        public String index;

    }

    public static class KeyDescriptor {

        @JacksonXmlProperty(localName = "use", isAttribute = true)
        public String use;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "KeyInfo")
        public List<KeyDescriptor.KeyInfo> keyInfos = new ArrayList<>();

        public static class KeyInfo {

            @JacksonXmlProperty(localName = "KeyName")
            public String keyName;

            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "X509Data")
            public List<KeyInfo.X509Data> x509Datas = new ArrayList<>();

            public static class X509Data {

                @JacksonXmlProperty(localName = "X509Certificate")
                public String x509Certificate;
            }
        }
    }

    public static class ArtifactResolutionService {

        @JacksonXmlProperty(localName = "Binding", isAttribute = true)
        public String binding;

        @JacksonXmlProperty(localName = "Location", isAttribute = true)
        public String location;

        @JacksonXmlProperty(localName = "index", isAttribute = true)
        public String index;
    }

    public static class SingleLogoutService {

        @JacksonXmlProperty(localName = "Binding", isAttribute = true)
        public String binding;

        @JacksonXmlProperty(localName = "Location", isAttribute = true)
        public String location;
    }

    public static class SingleSignOnService {

        @JacksonXmlProperty(localName = "Binding", isAttribute = true)
        public String binding;

        @JacksonXmlProperty(localName = "Location", isAttribute = true)
        public String location;
    }
}