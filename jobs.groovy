def git = "aliaksandr-lahutsin/testRepoForDSL"
def repo = "alahutsin"

def gitURL = "https://github.com/aliaksandr-lahutsin/testRepoForDSL.git"
def command = "git ls-remote -h $gitURL"

def proc = command.execute()
proc.waitFor()

if ( proc.exitValue() != 0 ) {
    println "Error, ${proc.err.text}"
    System.exit(-1)
}

def branches = proc.in.text.readLines().collect {
    it.replaceAll(/[a-z0-9]*\trefs\/heads\//, '')
}

job("MNTLAB-alahutsin-main-build-job") {
    logRotator {
        numToKeep(5)
        artifactNumToKeep(5)
    }
    parameters {
	choiceParam('BRANCH_NAME', ['alahutsin', 'master'], '')
        activeChoiceParam('BUILDS_TRIGGER') {
            filterable()
            choiceType('CHECKBOX')
            groovyScript {
                script('["MNTLAB-alahutsin-child1-build-job", "MNTLAB-alahutsin-child2-build-job", "MNTLAB-alahutsin-child3-build-job", "MNTLAB-alahutsin-child4-build-job"]')
            }
        }
    }
    scm {
        github(git, '$BRANCH_NAME')
    }
    triggers {
        scm('H/5 * * * *')
    }
    steps {
        downstreamParameterized {
            trigger('$BUILDS_TRIGGER') {
                block {
                    buildStepFailure('FAILURE')
                    failure('FAILURE')
                    unstable('UNSTABLE')
                }
               parameters {
                    currentBuild()
		}
	    }
	}	
        shell('chmod +x do.sh && ./do.sh > output.log && cat output.log && tar -czf ${BRANCH_NAME}_dsl_do.tar.gz output.log')
    }
    publishers { 
	archiveArtifacts('output.log')
    }



}

1.upto(4) {
job("MNTLAB-alahutsin-child${it}-build-job") {
    logRotator {
        numToKeep(5)
        artifactNumToKeep(5)
    }
    parameters {
	choiceParam('BRANCH_NAME', branches, '')
    }
    scm {
        github(git, '$BRANCH_NAME')
    }
    steps {
        shell('chmod +x do.sh && ./do.sh > output.log && cat output.log && tar -czf  ${BRANCH_NAME}_dsl_do.tar.gz output.log jobs.groovy do.sh')
    }
    publishers { 
        archiveArtifacts {
            pattern('output.log')
            pattern('${BRANCH_NAME}_dsl_do.tar.gz')
            onlyIfSuccessful()
   }
  }
 }
}
