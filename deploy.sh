#!/bin/bash
set -o pipefail # pipe fails with first failing command
set -o nounset # fail when unset vars are read
set -o errexit # exit script on failed command

export CDK_DIR=deployment/aws
export K8S_FOLDER=deployment/k8s

all() {
    01_aws_provision_infra
    02_build_and_upload_images
    03_k8s_services_deploy
}

01_aws_provision_infra() {
    _log_info "Provision AWS infra via CDK..."
    pushd deployment/aws
    cdk deploy --all --outputs-file cdk-outputs.json --require-approval=never
    popd

    _log_info "Configure ~/.kube/config to connect to cluster..."
    eval $(_source_cdk_output "eksclusterConfigCommand")
}

02_build_and_upload_images() {
    export ECR_REPOSITORY=$(_source_cdk_output "ECRRegistry" | cut -d "/" -f 1)

    aws ecr get-login-password --region eu-west-3 | docker login --username AWS --password-stdin $ECR_REPOSITORY
    ./gradlew :app:build --info -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true -Dquarkus.container-image.registry=$ECR_REPOSITORY
}

03_k8s_services_deploy() {
    export ECR_REPOSITORY=$(_source_cdk_output "ECRRegistry" | cut -d "/" -f 1)

    _log_start "Deploying services..."
    envsubst < $K8S_FOLDER/services.yaml | kubectl apply -f -
    
    kubectl wait --for=jsonpath='{.status.loadBalancer.ingress}' ingress/ingress-aws-playground

    export PUBLIC_ENDPOINT=$(kubectl get ingress ingress-aws-playground --output jsonpath='{.status.loadBalancer.ingress[0].hostname}')
    _log_info "Access @ http://$PUBLIC_ENDPOINT/hello"

    _log_ok "Deployed"
}

_source_cdk_output() {
    jq -r ".\"aws-playground\" | to_entries[] | select(.key | startswith(\"$1\")) | .value" $CDK_DIR/cdk-outputs.json
}

destroy() {
    export ECR_REPOSITORY=$(_source_cdk_output "ECRRegistry" | cut -d "/" -f 1)
    envsubst < $K8S_FOLDER/services.yaml | kubectl delete -f - || true

    pushd deployment/aws
    cdk destroy --require-approval=never
    popd
}

_kubectl_wait() {
    kubectl wait --for=condition=complete --timeout=90s $1 &
    completion_pid=$!

    kubectl wait --for=condition=failed --timeout=90s $1 && exit 1 &
    failure_pid=$!

    _log_info "Waiting for completion of $1"

    wait -n $completion_pid $failure_pid

    exit_code=$?

    return $exit_code
}

_log_start() {
    printf "%s" "$1"
}

_log_fail() {
    printf " \E[31m%s\E[0m\n" "Failed"
}

_log_ok() {
    printf " \E[32m%s\E[0m\n" "Ok"
}

_log_info() {
    printf "\E[1;37m%s\E[0;0m\n" "$1"
}

_log_warning() {
    printf "\E[1;33m%s\E[0;0m\n" "$1"
}

set +o nounset
if [ -n "$COMP_LINE" ]
then
    compgen -A function | grep -v "^_" | tr '\n' ' '
elif [[ $1 != "" ]];
then
    set -o nounset
    FUNC=$1
    shift
    $FUNC $@
else
    set -o nounset
    echo -e "\E[1;37musage: deploy.sh <command> [args]\E[0;0m"
    echo "commands are:"

    for c in $(compgen -A function);
    do
        if [[ $c != _* ]];
        then
            echo " * $c"
        fi
    done

    _log_warning "Tip: Run 'complete -C $(pwd)/deploy.sh deploy.sh' to get bash completion!"
fi