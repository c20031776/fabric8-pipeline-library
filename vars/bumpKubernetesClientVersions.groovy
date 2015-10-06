def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def project = 'kubernetes-client'
  node {
    ws (project){
      withEnv(["PATH+MAVEN=${tool 'maven-3.3.1'}/bin"]) {

        def flow = new io.fabric8.Release()
        flow.setupWorkspace (project)

        def uid = UUID.randomUUID().toString()
        sh "git checkout -b versionUpdate${uid}"

        try{
          // bump dependency versions from the previous stage
          def kubernetesModelVersion = flow.getReleaseVersion "kubernetes-model"
          flow.searchAndReplaceMavenVersionProperty("<kubernetes.model.version>", kubernetesModelVersion)

        } catch (err) {
          echo "Already on the latest versions of kubernetes-model"
          // only make a pull request if we've updated a version
          return
        }

        sh "git push origin versionUpdate${uid}"
        return flow.createPullRequest("[CD] Update release dependencies")
      }
    }
  }
}
