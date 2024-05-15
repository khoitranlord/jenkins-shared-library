ARG JENKINS_AGENT_VERSION="3206.vb_15dcf73f6a_9-2-jdk17"

FROM jenkins/inbound-agent:${JENKINS_AGENT_VERSION}

ARG AWSCLI_VERSION="2.15.10"
ARG AWS_IAM_AUTHENTICATOR_VERSION="0.6.14"
ARG CRANE_VERSION="0.17.0"
ARG HELM_VERSION="3.13.3"
ARG KUBECTL_VERSION="1.28.5"
ARG FLYWAY_VERSION="9.16.1"
ARG TERRAFORM_VERSION="1.6.6"
ARG TERRAGRUNT_VERSION="0.55.16"


MAINTAINER Infra Team

USER root

RUN apt-get update && \
    DEBIAN_FRONTEND="noninteractive" apt-get install -y -o Dpkg::Options::="--force-confnew" --no-install-recommends \
      curl \
      wget \
      telnet\
      tree \
      openssh-server\
      libssl-dev \
      jq \
      software-properties-common \
      git \
      build-essential \
      gettext-base \
      ca-certificates \
      curl \
      gnupg \
      lsb-release \
      unzip \
    && sed -i 's|session    required     pam_loginuid.so|session    optional     pam_loginuid.so|g' /etc/pam.d/sshd \
    && apt-get -q autoremove \
    && apt-get -q clean -y && rm -rf /var/lib/apt/lists/* && rm -f /var/cache/apt/*.bin

RUN curl https://awscli.amazonaws.com/awscli-exe-linux-x86_64-${AWSCLI_VERSION}.zip -o awscliv2.zip && unzip awscliv2.zip && ./aws/install

RUN wget -O /tmp/aws-iam-authenticator https://github.com/kubernetes-sigs/aws-iam-authenticator/releases/download/v${AWS_IAM_AUTHENTICATOR_VERSION}/aws-iam-authenticator_${AWS_IAM_AUTHENTICATOR_VERSION}_linux_amd64 && \
    cd /tmp && \
    mv aws-iam-authenticator /usr/local/bin/aws-iam-authenticator && \
    chmod +x /usr/local/bin/aws-iam-authenticator

# install helm
RUN curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 && \
    chmod 700 get_helm.sh && \
    ./get_helm.sh --version v${HELM_VERSION}

# install kubectl
RUN curl -LO https://dl.k8s.io/release/v${KUBECTL_VERSION}/bin/linux/amd64/kubectl && \
    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# install crane, needed to retag the image
RUN curl -L https://github.com/google/go-containerregistry/releases/download/v${CRANE_VERSION}/go-containerregistry_Linux_x86_64.tar.gz -o crane.tar.gz && \
    mkdir crane && \
    tar -xzf crane.tar.gz -C crane/ && \
    cp crane/*rane /usr/local/bin/ && \
    rm -rf crane*

## Install flyway
RUN cd ~ &&\
    wget -qO- "https://download.red-gate.com/maven/release/org/flywaydb/enterprise/flyway-commandline/${FLYWAY_VERSION}/flyway-commandline-${FLYWAY_VERSION}-linux-x64.tar.gz" | tar -xz && ln -s `pwd`/flyway-${FLYWAY_VERSION}/flyway /usr/local/bin/flyway

# Install terraform
RUN curl -L https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip -o terraform_${TERRAFORM_VERSION}_linux_amd64.zip &&\
    unzip terraform_${TERRAFORM_VERSION}_linux_amd64.zip &&\
    mv terraform /usr/local/bin/ &&\
    rm -f terraform_${TERRAFORM_VERSION}_linux_amd64.zip

# Install terragrunt
RUN curl -L https://github.com/gruntwork-io/terragrunt/releases/download/v${TERRAGRUNT_VERSION}/terragrunt_linux_amd64 -o  /usr/local/bin/terragrunt &&\
    chmod +x /usr/local/bin/terragrunt
