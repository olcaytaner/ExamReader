# ExamReader

### Overview  
ExamReader is a Java-based tool for analyzing and grading programming exam submissions.  
It compares student codes with reference solutions and visualizes structures with Graphviz.  

It generates:  
- AST (Abstract Syntax Tree) 
- CFG (Control Flow Graph) 
- DDG (Data Dependency Graph)  

### Features   
- Line-by-line token comparison  
- Graph outputs (AST, CFG, DDG) in .png format  
- Colored highlights for matched/mismatched lines  
- Simple UI for graph viewing
- Variable matching

### Structure  
src/
├── Exam/ → core logic (Exam, Question, Assessment, StudentCode, RefCode, Code, Variable)
├── Graph/ → graph building & Graphviz export
└── UI/ → visualization interface
