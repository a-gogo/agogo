export interface DeploymentAction {
  confirmPossible: boolean;
  rejectPossible: boolean;
  cancelPossible: boolean;
  redeployPossible: boolean;
  hasLogFiles: boolean;
  editPossible: boolean;
}
