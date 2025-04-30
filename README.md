# NoPend

- `src`: the implementation of baseline methods and NoPend, which is a maven project
- `data`: 
  - `raw`: the raw data of 10 batch, each run produces a `json` file.
  - `results`: `csv` table after processing the raw data
- `scripts`: 
  - `benchmark-preprocess`: processing the dataset for benchmark use
  - `data-process`: processing the raw data
  - `evaluation`: run all methods on all SUTs for collecting evaluation raw data
