export type FieldType = "text" | "email" | "number" | "currency" | "date" | "datetime" | "textarea" | "lookup";

/** Form-only field kinds on top of the display types: polymorphic activity links + datetime. */
export type FormFieldType = FieldType | "relatedTo" | "who";

export type FormField = {
  name: string; // submit key (e.g. "stageId", "accountId"); for relatedTo/who it's a synthetic label key
  label: string;
  type: FormFieldType;
  required?: boolean;
  lookupCategory?: string; // a lookup table category (e.g. "deal_stage")
  lookupEndpoint?: string; // a record FK (e.g. "accounts")
};

export type ColumnDef = { key: string; label: string; type?: FieldType; primary?: boolean };
export type DetailField = { key: string; label: string; type?: FieldType };

/** A list filter. `key` is the API query-param base (range kinds append Min/Max or From/To). */
export type FilterDef = {
  key: string;
  label: string;
  kind: "lookup" | "numberRange" | "dateRange" | "owner";
  lookupCategory?: string; // for kind "lookup"
};

export type ModuleConfig = {
  key: string;
  endpoint: string;
  singular: string;
  plural: string;
  icon: string;
  perm: string; // permission prefix (LEAD/ACCOUNT/CONTACT/DEAL)
  title: (r: any) => string;
  subtitle?: (r: any) => string;
  columns: ColumnDef[];
  filters?: FilterDef[];
  kanban?: { field: string; idField: string; lookupCategory: string; sumField?: string };
  stageProgress?: { field: string; idField: string; lookupCategory: string };
  calendar?: { dateField: string }; // enables the Calendar view mode (date/datetime field)
  summaryChips: DetailField[];
  sections: { title: string; fields: DetailField[] }[];
  form: FormField[];
  related: { label: string; endpoint: string; foreignKey: string }[];
};

const addr = (p: string, label: string): DetailField[] => [
  { key: `${p}Street`, label: `${label} street` }, { key: `${p}City`, label: `${label} city` },
  { key: `${p}State`, label: `${label} state` }, { key: `${p}Country`, label: `${label} country` },
  { key: `${p}Code`, label: `${label} code` },
];

export const MODULES: Record<string, ModuleConfig> = {
  deals: {
    key: "deals", endpoint: "deals", singular: "Deal", plural: "Deals", icon: "Handshake", perm: "DEAL",
    title: (r) => r.name, subtitle: (r) => r.accountName ?? "",
    columns: [
      { key: "name", label: "Deal", primary: true }, { key: "accountName", label: "Account" },
      { key: "stage", label: "Stage" }, { key: "amount", label: "Amount", type: "currency" },
      { key: "probability", label: "Prob", type: "number" }, { key: "closingDate", label: "Close date", type: "date" },
      { key: "ownerName", label: "Owner" },
    ],
    filters: [
      { key: "stageId", label: "Stage", kind: "lookup", lookupCategory: "deal_stage" },
      { key: "typeId", label: "Type", kind: "lookup", lookupCategory: "deal_type" },
      { key: "leadSourceId", label: "Lead source", kind: "lookup", lookupCategory: "lead_source" },
      { key: "amount", label: "Amount", kind: "numberRange" },
      { key: "closing", label: "Closing date", kind: "dateRange" },
      { key: "ownerId", label: "Owner", kind: "owner" },
    ],
    kanban: { field: "stage", idField: "stageId", lookupCategory: "deal_stage", sumField: "amount" },
    stageProgress: { field: "stage", idField: "stageId", lookupCategory: "deal_stage" },
    summaryChips: [
      { key: "amount", label: "Deal value", type: "currency" }, { key: "stage", label: "Stage" },
      { key: "probability", label: "Probability", type: "number" }, { key: "expectedRevenue", label: "Expected", type: "currency" },
      { key: "closingDate", label: "Close date", type: "date" },
    ],
    sections: [
      { title: "Deal information", fields: [
        { key: "name", label: "Deal name" }, { key: "accountName", label: "Account" }, { key: "contactName", label: "Contact" },
        { key: "type", label: "Type" }, { key: "leadSource", label: "Lead source" }, { key: "nextStep", label: "Next step" },
        { key: "amount", label: "Amount", type: "currency" }, { key: "expectedRevenue", label: "Expected revenue", type: "currency" },
        { key: "probability", label: "Probability (%)", type: "number" }, { key: "closingDate", label: "Closing date", type: "date" },
        { key: "reasonForLoss", label: "Reason for loss" }, { key: "ownerName", label: "Owner" },
      ]},
      { title: "Description", fields: [{ key: "description", label: "Description", type: "textarea" }] },
    ],
    form: [
      { name: "name", label: "Deal name", type: "text", required: true },
      { name: "stageId", label: "Stage", type: "lookup", required: true, lookupCategory: "deal_stage" },
      { name: "amount", label: "Amount", type: "currency" },
      { name: "closingDate", label: "Closing date", type: "date" },
      { name: "accountId", label: "Account", type: "lookup", lookupEndpoint: "accounts" },
      { name: "contactId", label: "Contact", type: "lookup", lookupEndpoint: "contacts" },
      { name: "typeId", label: "Type", type: "lookup", lookupCategory: "deal_type" },
      { name: "leadSourceId", label: "Lead source", type: "lookup", lookupCategory: "lead_source" },
      { name: "nextStep", label: "Next step", type: "text" },
      { name: "description", label: "Description", type: "textarea" },
    ],
    related: [],
  },

  leads: {
    key: "leads", endpoint: "leads", singular: "Lead", plural: "Leads", icon: "UserPlus", perm: "LEAD",
    title: (r) => r.fullName || r.lastName, subtitle: (r) => r.company ?? "",
    columns: [
      { key: "fullName", label: "Name", primary: true }, { key: "company", label: "Company" },
      { key: "email", label: "Email" }, { key: "leadStatus", label: "Status" },
      { key: "leadSource", label: "Source" }, { key: "ownerName", label: "Owner" },
    ],
    filters: [
      { key: "leadStatusId", label: "Status", kind: "lookup", lookupCategory: "lead_status" },
      { key: "leadSourceId", label: "Source", kind: "lookup", lookupCategory: "lead_source" },
      { key: "ratingId", label: "Rating", kind: "lookup", lookupCategory: "rating" },
      { key: "industryId", label: "Industry", kind: "lookup", lookupCategory: "industry" },
      { key: "ownerId", label: "Owner", kind: "owner" },
    ],
    kanban: { field: "leadStatus", idField: "leadStatusId", lookupCategory: "lead_status" },
    stageProgress: { field: "leadStatus", idField: "leadStatusId", lookupCategory: "lead_status" },
    summaryChips: [
      { key: "leadStatus", label: "Status" }, { key: "company", label: "Company" },
      { key: "rating", label: "Rating" }, { key: "leadSource", label: "Source" },
      { key: "annualRevenue", label: "Annual revenue", type: "currency" },
    ],
    sections: [
      { title: "Lead information", fields: [
        { key: "salutation", label: "Salutation" }, { key: "firstName", label: "First name" }, { key: "lastName", label: "Last name" },
        { key: "company", label: "Company" }, { key: "title", label: "Title" }, { key: "industry", label: "Industry" },
        { key: "email", label: "Email" }, { key: "phone", label: "Phone" }, { key: "mobile", label: "Mobile" }, { key: "website", label: "Website" },
        { key: "leadSource", label: "Lead source" }, { key: "leadStatus", label: "Lead status" }, { key: "rating", label: "Rating" },
        { key: "noOfEmployees", label: "No. of employees", type: "number" }, { key: "annualRevenue", label: "Annual revenue", type: "currency" },
        { key: "ownerName", label: "Owner" },
      ]},
      { title: "Description", fields: [{ key: "description", label: "Description", type: "textarea" }] },
    ],
    form: [
      { name: "salutationId", label: "Salutation", type: "lookup", lookupCategory: "salutation" },
      { name: "firstName", label: "First name", type: "text" },
      { name: "lastName", label: "Last name", type: "text", required: true },
      { name: "company", label: "Company", type: "text" },
      { name: "title", label: "Title", type: "text" },
      { name: "email", label: "Email", type: "email" },
      { name: "phone", label: "Phone", type: "text" },
      { name: "leadSourceId", label: "Lead source", type: "lookup", lookupCategory: "lead_source" },
      { name: "industryId", label: "Industry", type: "lookup", lookupCategory: "industry" },
      { name: "ratingId", label: "Rating", type: "lookup", lookupCategory: "rating" },
      { name: "description", label: "Description", type: "textarea" },
    ],
    related: [],
  },

  accounts: {
    key: "accounts", endpoint: "accounts", singular: "Account", plural: "Accounts", icon: "Building2", perm: "ACCOUNT",
    title: (r) => r.name, subtitle: (r) => r.industry ?? "",
    columns: [
      { key: "name", label: "Account", primary: true }, { key: "accountType", label: "Type" },
      { key: "industry", label: "Industry" }, { key: "phone", label: "Phone" },
      { key: "employees", label: "Employees", type: "number" }, { key: "ownerName", label: "Owner" },
    ],
    filters: [
      { key: "accountTypeId", label: "Type", kind: "lookup", lookupCategory: "account_type" },
      { key: "industryId", label: "Industry", kind: "lookup", lookupCategory: "industry" },
      { key: "ownershipId", label: "Ownership", kind: "lookup", lookupCategory: "ownership" },
      { key: "ratingId", label: "Rating", kind: "lookup", lookupCategory: "rating" },
      { key: "employees", label: "Employees", kind: "numberRange" },
      { key: "ownerId", label: "Owner", kind: "owner" },
    ],
    summaryChips: [
      { key: "accountType", label: "Type" }, { key: "industry", label: "Industry" },
      { key: "annualRevenue", label: "Annual revenue", type: "currency" }, { key: "employees", label: "Employees", type: "number" },
      { key: "rating", label: "Rating" },
    ],
    sections: [
      { title: "Account information", fields: [
        { key: "name", label: "Account name" }, { key: "accountSite", label: "Account site" }, { key: "accountType", label: "Type" },
        { key: "ownership", label: "Ownership" }, { key: "industry", label: "Industry" }, { key: "rating", label: "Rating" },
        { key: "phone", label: "Phone" }, { key: "website", label: "Website" }, { key: "employees", label: "Employees", type: "number" },
        { key: "annualRevenue", label: "Annual revenue", type: "currency" }, { key: "parentAccountName", label: "Parent account" },
        { key: "ownerName", label: "Owner" },
      ]},
      { title: "Billing address", fields: addr("billing", "Billing") },
      { title: "Shipping address", fields: addr("shipping", "Shipping") },
      { title: "Description", fields: [{ key: "description", label: "Description", type: "textarea" }] },
    ],
    form: [
      { name: "name", label: "Account name", type: "text", required: true },
      { name: "accountTypeId", label: "Type", type: "lookup", lookupCategory: "account_type" },
      { name: "industryId", label: "Industry", type: "lookup", lookupCategory: "industry" },
      { name: "ownershipId", label: "Ownership", type: "lookup", lookupCategory: "ownership" },
      { name: "ratingId", label: "Rating", type: "lookup", lookupCategory: "rating" },
      { name: "phone", label: "Phone", type: "text" },
      { name: "website", label: "Website", type: "text" },
      { name: "employees", label: "Employees", type: "number" },
      { name: "annualRevenue", label: "Annual revenue", type: "currency" },
      { name: "billingCity", label: "Billing city", type: "text" },
      { name: "billingCountry", label: "Billing country", type: "text" },
      { name: "description", label: "Description", type: "textarea" },
    ],
    related: [
      { label: "Contacts", endpoint: "contacts", foreignKey: "accountId" },
      { label: "Deals", endpoint: "deals", foreignKey: "accountId" },
    ],
  },

  contacts: {
    key: "contacts", endpoint: "contacts", singular: "Contact", plural: "Contacts", icon: "Users", perm: "CONTACT",
    title: (r) => r.fullName || r.lastName, subtitle: (r) => r.accountName ?? r.title ?? "",
    columns: [
      { key: "fullName", label: "Name", primary: true }, { key: "accountName", label: "Account" },
      { key: "title", label: "Title" }, { key: "email", label: "Email" },
      { key: "phone", label: "Phone" }, { key: "ownerName", label: "Owner" },
    ],
    filters: [
      { key: "leadSourceId", label: "Lead source", kind: "lookup", lookupCategory: "lead_source" },
      { key: "ownerId", label: "Owner", kind: "owner" },
    ],
    summaryChips: [
      { key: "accountName", label: "Account" }, { key: "title", label: "Title" },
      { key: "email", label: "Email" }, { key: "phone", label: "Phone" },
    ],
    sections: [
      { title: "Contact information", fields: [
        { key: "firstName", label: "First name" }, { key: "lastName", label: "Last name" }, { key: "accountName", label: "Account" },
        { key: "title", label: "Title" }, { key: "department", label: "Department" }, { key: "email", label: "Email" },
        { key: "phone", label: "Phone" }, { key: "mobile", label: "Mobile" }, { key: "leadSource", label: "Lead source" },
        { key: "reportingToName", label: "Reporting to" }, { key: "ownerName", label: "Owner" },
      ]},
      { title: "Mailing address", fields: addr("mailing", "Mailing") },
      { title: "Description", fields: [{ key: "description", label: "Description", type: "textarea" }] },
    ],
    form: [
      { name: "firstName", label: "First name", type: "text" },
      { name: "lastName", label: "Last name", type: "text", required: true },
      { name: "accountId", label: "Account", type: "lookup", lookupEndpoint: "accounts" },
      { name: "title", label: "Title", type: "text" },
      { name: "department", label: "Department", type: "text" },
      { name: "email", label: "Email", type: "email" },
      { name: "phone", label: "Phone", type: "text" },
      { name: "mobile", label: "Mobile", type: "text" },
      { name: "leadSourceId", label: "Lead source", type: "lookup", lookupCategory: "lead_source" },
      { name: "description", label: "Description", type: "textarea" },
    ],
    related: [{ label: "Deals", endpoint: "deals", foreignKey: "contactId" }],
  },

  tasks: {
    key: "tasks", endpoint: "tasks", singular: "Task", plural: "Tasks", icon: "CheckSquare", perm: "TASK",
    title: (r) => r.subject, subtitle: (r) => r.whatName ?? r.status ?? "",
    columns: [
      { key: "subject", label: "Subject", primary: true }, { key: "status", label: "Status" },
      { key: "priority", label: "Priority" }, { key: "dueDate", label: "Due", type: "date" },
      { key: "whatName", label: "Related to" }, { key: "ownerName", label: "Owner" },
    ],
    filters: [
      { key: "statusId", label: "Status", kind: "lookup", lookupCategory: "task_status" },
      { key: "priorityId", label: "Priority", kind: "lookup", lookupCategory: "task_priority" },
      { key: "ownerId", label: "Owner", kind: "owner" },
    ],
    kanban: { field: "status", idField: "statusId", lookupCategory: "task_status" },
    stageProgress: { field: "status", idField: "statusId", lookupCategory: "task_status" },
    calendar: { dateField: "dueDate" },
    summaryChips: [
      { key: "status", label: "Status" }, { key: "priority", label: "Priority" },
      { key: "dueDate", label: "Due date", type: "date" }, { key: "whatName", label: "Related to" },
    ],
    sections: [
      { title: "Task information", fields: [
        { key: "subject", label: "Subject" }, { key: "status", label: "Status" }, { key: "priority", label: "Priority" },
        { key: "dueDate", label: "Due date", type: "date" }, { key: "whatName", label: "Related to" },
        { key: "whoName", label: "Contact" }, { key: "ownerName", label: "Owner" },
      ]},
      { title: "Description", fields: [{ key: "description", label: "Description", type: "textarea" }] },
    ],
    form: [
      { name: "subject", label: "Subject", type: "text", required: true },
      { name: "statusId", label: "Status", type: "lookup", lookupCategory: "task_status" },
      { name: "priorityId", label: "Priority", type: "lookup", lookupCategory: "task_priority" },
      { name: "dueDate", label: "Due date", type: "date" },
      { name: "what", label: "Related to", type: "relatedTo" },
      { name: "who", label: "Contact", type: "who" },
      { name: "description", label: "Description", type: "textarea" },
    ],
    related: [],
  },

  meetings: {
    key: "meetings", endpoint: "meetings", singular: "Meeting", plural: "Meetings", icon: "CalendarDays", perm: "MEETING",
    title: (r) => r.title, subtitle: (r) => r.whatName ?? r.location ?? "",
    columns: [
      { key: "title", label: "Title", primary: true }, { key: "status", label: "Status" },
      { key: "startAt", label: "Start", type: "datetime" }, { key: "location", label: "Location" },
      { key: "whatName", label: "Related to" }, { key: "ownerName", label: "Owner" },
    ],
    filters: [
      { key: "statusId", label: "Status", kind: "lookup", lookupCategory: "meeting_status" },
      { key: "ownerId", label: "Owner", kind: "owner" },
    ],
    kanban: { field: "status", idField: "statusId", lookupCategory: "meeting_status" },
    stageProgress: { field: "status", idField: "statusId", lookupCategory: "meeting_status" },
    calendar: { dateField: "startAt" },
    summaryChips: [
      { key: "status", label: "Status" }, { key: "startAt", label: "Start", type: "datetime" },
      { key: "endAt", label: "End", type: "datetime" }, { key: "location", label: "Location" },
    ],
    sections: [
      { title: "Meeting information", fields: [
        { key: "title", label: "Title" }, { key: "status", label: "Status" }, { key: "location", label: "Location" },
        { key: "startAt", label: "Start", type: "datetime" }, { key: "endAt", label: "End", type: "datetime" },
        { key: "whatName", label: "Related to" }, { key: "whoName", label: "Contact" }, { key: "ownerName", label: "Owner" },
      ]},
      { title: "Description", fields: [{ key: "description", label: "Description", type: "textarea" }] },
    ],
    form: [
      { name: "title", label: "Title", type: "text", required: true },
      { name: "statusId", label: "Status", type: "lookup", lookupCategory: "meeting_status" },
      { name: "location", label: "Location", type: "text" },
      { name: "startAt", label: "Start", type: "datetime" },
      { name: "endAt", label: "End", type: "datetime" },
      { name: "what", label: "Related to", type: "relatedTo" },
      { name: "who", label: "Contact", type: "who" },
      { name: "description", label: "Description", type: "textarea" },
    ],
    related: [],
  },

  calls: {
    key: "calls", endpoint: "calls", singular: "Call", plural: "Calls", icon: "Phone", perm: "CALL",
    title: (r) => r.subject, subtitle: (r) => r.whatName ?? r.callType ?? "",
    columns: [
      { key: "subject", label: "Subject", primary: true }, { key: "callType", label: "Type" },
      { key: "startAt", label: "When", type: "datetime" }, { key: "durationMinutes", label: "Min", type: "number" },
      { key: "callResult", label: "Result" }, { key: "ownerName", label: "Owner" },
    ],
    filters: [
      { key: "callTypeId", label: "Type", kind: "lookup", lookupCategory: "call_type" },
      { key: "callResultId", label: "Result", kind: "lookup", lookupCategory: "call_result" },
      { key: "ownerId", label: "Owner", kind: "owner" },
    ],
    kanban: { field: "callResult", idField: "callResultId", lookupCategory: "call_result" },
    calendar: { dateField: "startAt" },
    summaryChips: [
      { key: "callType", label: "Type" }, { key: "startAt", label: "When", type: "datetime" },
      { key: "durationMinutes", label: "Duration (min)", type: "number" }, { key: "callResult", label: "Result" },
    ],
    sections: [
      { title: "Call information", fields: [
        { key: "subject", label: "Subject" }, { key: "callType", label: "Type" }, { key: "startAt", label: "Start", type: "datetime" },
        { key: "durationMinutes", label: "Duration (min)", type: "number" }, { key: "callPurpose", label: "Purpose" },
        { key: "callResult", label: "Result" }, { key: "whatName", label: "Related to" }, { key: "whoName", label: "Contact" },
        { key: "ownerName", label: "Owner" },
      ]},
      { title: "Description", fields: [{ key: "description", label: "Description", type: "textarea" }] },
    ],
    form: [
      { name: "subject", label: "Subject", type: "text", required: true },
      { name: "callTypeId", label: "Type", type: "lookup", lookupCategory: "call_type" },
      { name: "startAt", label: "Start", type: "datetime" },
      { name: "durationMinutes", label: "Duration (min)", type: "number" },
      { name: "callPurposeId", label: "Purpose", type: "lookup", lookupCategory: "call_purpose" },
      { name: "callResultId", label: "Result", type: "lookup", lookupCategory: "call_result" },
      { name: "what", label: "Related to", type: "relatedTo" },
      { name: "who", label: "Contact", type: "who" },
      { name: "description", label: "Description", type: "textarea" },
    ],
    related: [],
  },
};

export const MODULE_ORDER = ["deals", "leads", "accounts", "contacts", "tasks", "meetings", "calls"];

/** Modules selectable as an activity's polymorphic "what" (related-to) and "who" targets. */
export const RELATED_TO_MODULES = ["accounts", "deals", "contacts", "leads"];
export const WHO_MODULES = ["contacts", "leads"];
