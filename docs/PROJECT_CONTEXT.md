# MicRouter Project Context

## Documentation Policy

All product, design, technical research, architecture decisions, MVP planning, implementation notes, testing results, and handover information should be maintained in the repository docs folder.

The repository is the source of truth.

## Documentation Hierarchy

MicRouter_Product_Blueprint.md is the authoritative product document.
Other docs provide research detail, engineering history, or historical records.
When documents conflict, the Blueprint wins.

## Current Documentation Map

- MicRouter_Product_Blueprint.md (authoritative)
  - Problem argumentation and evidence
  - Competitive landscape and borrowed lessons
  - Technical feasibility verdict table
  - Layered blueprint: MVP / V1 / V2
  - Success metrics, risks, permanent non-goals

- MicRouter_Framework_Architecture.md
  - Five-layer system framework
  - User personas, information architecture, design philosophy

- Android_Audio_Routing_Research.md
  - Android audio framework research
  - Routing mechanisms, API investigation

- Phase_Completion_Report.md
  - Development progress
  - Completed phases
  - Validation plan

- Product_Design_Handoff.md (historical record)
  - Original design handoff; superseded by Blueprint for product decisions

Removed after merge into Blueprint:

- MVP_Product_Model.md
- MicRouter_MVP_Product_Model.md

## Working Principle

Do not keep important project decisions only in chat history.

Every significant decision should be converted into repository documentation before implementation continues.
