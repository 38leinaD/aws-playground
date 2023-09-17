package de.dplatz;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.eks.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CDKStack extends Stack {

    private IVpc vpc;
    private IUser iamUser;

    public CDKStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);

        GetCallerIdentityResponse callerIdentity = StsClient.create().getCallerIdentity();
        String currentUserArn = callerIdentity.arn();

        System.out.println("Current user ARN: " + currentUserArn);

        iamUser = User.fromUserArn(this, "AIDAWN65RPUXVTPX3RWCK", currentUserArn);

        vpc = Vpc.fromLookup(this, "default", VpcLookupOptions.builder().isDefault(true).build());

        Repository repo = Repository.Builder.create(this, "app-ecr")
                .repositoryName("app")
                .build();

        CfnOutput.Builder.create(this, "ECR-Registry").value(repo.getRepositoryUri()).build();

        Role eksNodeGroupRole = Role.Builder.create(this, "eks-node-group-role")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .description("Example role...")
                .managedPolicies(List.of(
                        ManagedPolicy.fromManagedPolicyArn(this, "app-AmazonEKSWorkerNodePolicy", "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"),
                        ManagedPolicy.fromManagedPolicyArn(this, "app-AmazonEC2ContainerRegistryReadOnly", "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"),
                        ManagedPolicy.fromManagedPolicyArn(this, "app-AmazonEKS_CNI_Policy", "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy")
                ))
                .build();

        Role clusterAdminRole = Role.Builder.create(this, "eks-admin-role")
                .assumedBy(iamUser).build();

        Cluster eksCluster = Cluster.Builder.create(this,"eks-cluster")
                .vpc(vpc)
                .vpcSubnets(List.of(
                        SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build(),
                        SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build(),
                        SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build()))
                .defaultCapacity(2)
                .defaultCapacityInstance(InstanceType.of(InstanceClass.T3, InstanceSize.SMALL))
                .defaultCapacityType(DefaultCapacityType.EC2)
                .mastersRole(clusterAdminRole)
                .albController(AlbControllerOptions.builder()
                        .version(AlbControllerVersion.V2_5_1)
                        .build())
                .version(KubernetesVersion.V1_27).build();
    }
}
