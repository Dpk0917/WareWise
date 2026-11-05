# GitHub Collaboration Workflow

This workflow lets all three team members work on separate branches.

## 1. One-time setup (each team member)

Clone the repository and create your own branch.

### Ashu

``` bash
git clone https://github.com/<org>/<repo>.git
cd <repo>
git switch -c ashu
git push -u origin ashu
```

## 2. Everyday workflow (everyone)

Each person works on their own branch. Example for Ashu:

``` bash
# 1. Switch to your branch
git switch ashu

# 2. Get the latest main changes
git fetch origin
git merge origin/main

# 3. Write your code

# 4. Stage changes
git add .

# 5. Commit changes
git commit -m "Add product management"

# 6. Push your branch
git push origin ashu
```

Rahul and Deepak follow the same steps, replacing `ashu` with their own
branch names.

## 3. Merge branches into main

When a team member's work is ready, Deepak can merge that branch into
`main`.

``` bash
# 1. Switch to main
git switch main

# 2. Get the latest main
git pull origin main

# 3. Merge Ashu's branch
git fetch origin
git merge origin/ashu

# 4. Push updated main
git push origin main
```

Repeat for Rahul and Deepak when their work is ready:

``` bash
git fetch origin
git merge origin/rahul
git push origin main

git fetch origin
git merge origin/deepak
git push origin main
```

If conflicts occur, resolve them, stage the resolved files, and complete
the merge commit before pushing:

``` bash
git add .
git commit
git push origin main
```

## 4. Everyone syncs their branch with the latest main

After Deepak merges changes into `main`, everyone should update their
own branch.

### Ashu

``` bash
git switch ashu
git fetch origin
git merge origin/main
git push origin ashu
```

## 5. Team rules

1.  Everyone should commit and push code to their own branch.
2.  Only Deepak should merge completed work into `main`.
3.  Before starting new work, sync your branch with the latest `main`.
4.  Review changes and resolve merge conflicts before pushing.
5.  Do not force-push to `main` or overwrite another person's branch.
6.  Prefer GitHub Pull Requests: Ashu and Rahul open PRs into `main`,
    and Deepak reviews and merges them.