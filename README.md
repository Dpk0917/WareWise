# GitHub Collaboration Steps

**1. One-time setup (each person, on their own machine)**

```bash
git clone https://github.com/<org>/<repo>.git
cd <repo>
git checkout -b ashu     # rahul / deepak use their own names
```
**2. Everyday work — Ashu, Rahul, and Deepak on their own branches**

```bash
git checkout ashu                 # switch to your branch
git pull origin main              # optional: sync with latest main first

# write your code part

git add .
git commit -m "Describe what you changed"
git push origin ashu              # first time: git push -u origin ashu
```
**3. Getting a branch into main (Deepak has main access, so he does the merge)**

```bash
git checkout main
git pull origin main
git merge ashu                    # merge Ashu's branch into main
# resolve conflicts if any, then:
git push origin main
```
Repeat for `rahul` and `deepak`'s own branch when ready.

**4. Everyone syncs up with the new main**

```bash
git checkout main
git pull origin main
```
Then switch back to your own branch and continue working:

```bash
git checkout ashu
git merge main         
# optional: bring latest main into your branch to avoid conflicts later
```