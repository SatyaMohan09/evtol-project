// missionStatus.js

export const MissionStatus = Object.freeze({
  IDLE: "IDLE",
  ENROUTE: "ENROUTE",
  LANDING: "LANDING",
  PICKING_UP: "PICKING_UP",
  RETURN: "RETURN",
});

export class MissionStateMachine {
  constructor() {
    this.status = MissionStatus.IDLE;
    this.listeners = new Set();
  }

  setStatus(newStatus) {
  if (this.status === newStatus) return;

  this.status = newStatus;

  // notify internal listeners
  this.notify();

  // external hook (React/UI sync)
  if (this.onUpdate) {
    this.onUpdate(this.status);
  }
}
setUpdateCallback(fn) {
  this.onUpdate = fn;
}

  getStatus() {
    return this.status;
  }

  next() {
    switch (this.status) {
      case MissionStatus.IDLE:
        this.setStatus(MissionStatus.ENROUTE);
        break;

      case MissionStatus.ENROUTE:
        this.setStatus(MissionStatus.LANDING);
        break;

      case MissionStatus.LANDING:
        this.setStatus(MissionStatus.PICKING_UP);
        break;

      case MissionStatus.PICKING_UP:
        this.setStatus(MissionStatus.RETURN);
        break;

      case MissionStatus.RETURN:
        this.setStatus(MissionStatus.IDLE);
        break;

      default:
        this.setStatus(MissionStatus.IDLE);
    }
  }

  reset() {
    this.setStatus(MissionStatus.IDLE);
  }

  subscribe(fn) {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  notify() {
    this.listeners.forEach((fn) => fn(this.status));
  }
}
