import {
	ProcessCompletionDTO,
	ProcessDTO,
	ProcessInstanceLightDto,
	ProcessInstanceListDto,
	TaskDTO,
	TaskState
} from '../../src/app/shared/processservice/process.model';

const mockProcess_1: ProcessDTO = {
	originProcessId: 'XXX',
	processTemplate: '', // Not present in JSON
	name: {
		de: 'Race Across Switzerland',
		it: '[IT] Race Across Switzerland',
		fr: '[FR] Race Across Switzerland'
	},
	state: 'STARTED',
	processCompletion: {
		conclusion: 'conc',
		reason: 'reson',
		completedAt: ''
	} as ProcessCompletionDTO,
	createdAt: '2023-06-27T10:19:09.786578+02:00',
	modifiedAt: '2023-06-27T10:19:10.431462+02:00',
	tasks: [
		{
			originTaskId: '1',
			name: {
				de: 'Rennen starten',
				it: '[IT] Rennen starten',
				fr: '[FR] Rennen starten'
			},
			lifecycle: 'STATIC',
			cardinality: 'SINGLE_INSTANCE',
			state: TaskState.PLANNED,
			createdAt: '', // Not present in JSON
			plannedAt: '2023-06-27T10:19:09.763437+02:00',
			completedAt: null,
			plannedBy: [],
			completedBy: null,
			taskData: []
		} as TaskDTO,
		{
			originTaskId: '2',
			name: {
				de: 'Ziel passieren',
				it: '[IT] Ziel passieren',
				fr: '[FR] Ziel passieren'
			},
			lifecycle: 'STATIC',
			cardinality: 'SINGLE_INSTANCE',
			state: TaskState.PLANNED,
			createdAt: '',
			plannedAt: '2023-06-27T10:19:09.763482+02:00',
			completedAt: null,
			plannedBy: [],
			completedBy: null,
			taskData: []
		} as TaskDTO,
		{
			originTaskId: '3',
			name: {
				de: 'Rennstrecke validieren',
				it: '[IT] Rennstrecke validieren',
				fr: '[FR] Rennstrecke validieren'
			},
			lifecycle: 'STATIC',
			cardinality: 'SINGLE_INSTANCE',
			state: TaskState.PLANNED,
			createdAt: '',
			plannedAt: '2023-06-27T10:19:09.763496+02:00',
			completedAt: null,
			plannedBy: [],
			completedBy: null,
			taskData: []
		} as TaskDTO
	],
	messages: [],
	relations: [],
	processRelations: [],
	processData: [
		{
			key: 'race-car-number',
			value: 'string',
			role: 'some-role'
		},
		{
			key: 'race-id',
			value: 'New Race Across Switzerland',
			role: 'some-role'
		}
	],
	snapshot: false,
	snapshotCreatedAt: ''
};

const mockProcessList: ProcessInstanceListDto = {
	totalCount: 2,
	processInstanceLightDtoList: [
		{
			createdAt: "122",
			lastMessageCreatedAt: "111",
			state: "status1",
			originProcessId: "o1",
			name: {
				de: "Template Process 1",
				it: "[IT] Race Across Switzerland",
				fr: "[FR] Race Across Switzerland"
			},
		} as ProcessInstanceLightDto,
		{
			createdAt: "122",
			lastMessageCreatedAt: "111",
			state: "status2",
			originProcessId: "o2",
			name: {
				de: "Template Process 2",
				it: "[IT] Template Process 2",
				fr: "[FR] Template Process 2"
			},
		} as ProcessInstanceLightDto
	]

}

export {mockProcess_1, mockProcessList};


