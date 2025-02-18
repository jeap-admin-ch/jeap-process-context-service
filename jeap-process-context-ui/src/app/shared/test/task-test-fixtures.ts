import { TaskDTO, TaskState } from '../processservice/process.model';

export class TaskTestFixtures {
	private static readonly taskPlannedStatic: TaskDTO = {
		originTaskId: '1',
		name: {
			de: 'Planned Static Task #1',
			it: '[it] Planned Static Task #1',
			fr: '[fr] Planned Static Task #1'
		},
		lifecycle: 'STATIC',
		cardinality: 'SINGLE_INSTANCE',
		state: TaskState.PLANNED,
		plannedAt: '2023-10-23T10:00:00.0+02:00',
		completedAt: null,
		plannedBy: [{ key: 'user1', value: 'User One', label: { de: 'Benutzer Eins' } }],
		completedBy: [],
		taskData: []
	};

	private static readonly taskPlannedDynamic_1: TaskDTO = {
		originTaskId: '2',
		name: {
			de: 'Planned Dynamic Task #2',
			it: '[it] Planned Dynamic Task #2',
			fr: '[fr] Planned Dynamic Task #2'
		},
		lifecycle: 'DYNAMIC',
		cardinality: 'SINGLE_INSTANCE',
		state: TaskState.PLANNED,
		plannedAt: '2023-10-24T09:00:00.0+02:00',
		completedAt: null,
		plannedBy: [{ key: 'user2', value: 'User Two', label: { de: 'Benutzer Zwei' } }],
		completedBy: [],
		taskData: []
	};

	private static readonly taskPlannedDynamic_2: TaskDTO = {
		originTaskId: '3',
		name: {
			de: 'Planned Dynamic Task #3',
			it: '[it] Planned Dynamic Task #3',
			fr: '[fr] Planned Dynamic Task #3'
		},
		lifecycle: 'DYNAMIC',
		cardinality: 'SINGLE_INSTANCE',
		state: TaskState.PLANNED,
		plannedAt: '2023-10-25T09:00:00.0+02:00',
		completedAt: null,
		plannedBy: [{ key: 'user3', value: 'User Three', label: { de: 'Benutzer Drei' } }],
		completedBy: [],
		taskData: []
	};

	private static readonly taskCompleted_1: TaskDTO = {
		originTaskId: '4',
		name: {
			de: 'Completed Task #4',
			it: '',
			fr: ''
		},
		lifecycle: 'STATIC',
		cardinality: 'SINGLE_INSTANCE',
		state: TaskState.COMPLETED,
		plannedAt: '2023-06-27T10:19:09.763437+02:00',
		completedAt: '2023-10-24T11:00:00.0+02:00',
		plannedBy: [{ key: 'user4', value: 'User Four', label: { de: 'Benutzer Vier' } }],
		completedBy: [{ key: 'user4', value: 'User Four', label: { de: 'Benutzer Vier' } }],
		taskData: []
	};

	private static readonly taskCompleted_2: TaskDTO = {
		originTaskId: '5',
		name: {
			de: 'Completed Task #5',
			it: '',
			fr: ''
		},
		lifecycle: 'STATIC',
		cardinality: 'SINGLE_INSTANCE',
		state: TaskState.COMPLETED,
		plannedAt: '2023-05-27T10:19:09.763437+02:00',
		completedAt: '2023-10-24T12:00:00.0+02:00',
		plannedBy: [{ key: 'user5', value: 'User Five', label: { de: 'Benutzer Fünf' } }],
		completedBy: [{ key: 'user5', value: 'User Five', label: { de: 'Benutzer Fünf' } }],
		taskData: []
	};

	private static readonly taskNotPlanned: TaskDTO = {
		originTaskId: '6',
		name: {
			de: 'Not Planned Task #6',
			it: '',
			fr: ''
		},
		lifecycle: 'STATIC',
		cardinality: 'SINGLE_INSTANCE',
		state: TaskState.NOT_PLANNED,
		plannedAt: '2023-06-27T10:19:09.763437+02:00',
		completedAt: '2023-06-27T10:19:09.763437+02:00',
		plannedBy: [{ key: 'user6', value: 'User Six', label: { de: 'Benutzer Sechs' } }],
		completedBy: [{ key: 'user6', value: 'User Six', label: { de: 'Benutzer Sechs' } }],
		taskData: []
	};

	static taskPlannedStaticList: TaskDTO[] = [this.taskPlannedStatic];
	static taskPlannedDynamicList: TaskDTO[] = [this.taskPlannedDynamic_1, this.taskPlannedDynamic_2];
	static taskPlannedList: TaskDTO[] = [
		this.taskPlannedStatic,
		this.taskPlannedDynamic_1,
		this.taskPlannedDynamic_2,
		this.taskCompleted_1,
		this.taskCompleted_2,
		this.taskNotPlanned
	];
}
