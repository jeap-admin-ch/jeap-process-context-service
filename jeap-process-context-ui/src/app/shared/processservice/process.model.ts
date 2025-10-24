export interface ProcessDTO {
	originProcessId: string;
	processTemplate: string;
	name: any;
	state: string;
	processCompletion: ProcessCompletionDTO;
	createdAt: string;
	modifiedAt: string;
	tasks: TaskDTO[];
	milestones: MilestoneDTO[];
	messages: MessageDTO[];
	relations: RelationDTO[];
	processRelations: ProcessRelationDTO[];
	processData: ProcessDataDTO[];
	snapshot: boolean;
	snapshotCreatedAt: string;
}

export interface ProcessRelationDTO {
	relation: any;
	processName: any;
	processId: string;
	processState: string;
}

export interface ProcessCompletionDTO {
	conclusion: string
	reason: any;
	completedAt: string;
}

export interface RelationDTO {
	subjectType: string;
	subjectId: string;
	objectType: string;
	objectId: string;
	predicateType: string;
	createdAt: string;
}

export interface ProcessDataDTO {
	key: string;
	value: string;
	role: string;
}

export interface TaskDTO {
	originTaskId: string;
	name: I18n;
	state: TaskState;
	createdAt: string;
	plannedAt: string;
	completedAt: string | null;
	lifecycle: string;
	cardinality: string;
	plannedBy: UserData[];
	completedBy: UserData[] | null;
	taskData: TaskDataDto[];
}

export interface UserData {
	key: string;
	value: string;
	label: { [key: string]: string };
}

export interface TaskDataDto {
	key: string;
	value: string;
	labels: { [key: string]: string };
}

export interface I18n {
	[key: string]: string;  // Index signature allowing string keys (for languages)
	de: string;
	fr: string;
	it: string;
}

export enum TaskState {
	NOT_PLANNED = "NOT_PLANNED",
	PLANNED = "PLANNED",
	COMPLETED = "COMPLETED",
	NOT_REQUIRED = "NOT_REQUIRED",
	DELETED = "DELETED",
	UNKNOWN = "UNKNOWN"
}

export interface MilestoneDTO {
	name: string;
	state: string;
	reachedAt: string;
}

export interface MessageDTO {
	id: string;
	name: string;
	createdAt: string;
	receivedAt: string;
	relatedOriginTaskIds: Set<string>[];
	messageData: MessageDataDTO[];
	traceId: string;
}

export interface MessageDataDTO {
	key: string;
	value: string;
	role: string;
}

export interface ProcessInstanceLightDto {
	originProcessId: string;
	createdAt: string;
	name: any;
	state: string;
	lastMessageCreatedAt: string;
}

export interface ProcessInstanceListDto {
	totalCount: number;
	processInstanceLightDtoList: ProcessInstanceLightDto[];
}
