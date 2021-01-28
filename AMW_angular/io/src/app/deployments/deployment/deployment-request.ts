import { AppWithVersion } from './app-with-version';
import { DeploymentParameter } from './deployment-parameter';

export interface DeploymentRequest {
  appServerName: string;
  contextIds: number[];
  releaseName: string;
  requestOnly: boolean;
  simulate: boolean;
  executeShakedownTest: boolean;
  neighbourhoodTest: boolean;
  sendEmail: boolean;
  appsWithVersion: AppWithVersion[];
  stateToDeploy: number;
  deploymentDate: number;
  deploymentParameters: DeploymentParameter[];
}
